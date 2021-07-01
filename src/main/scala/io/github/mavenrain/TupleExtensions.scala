package io.github.mavenrain

trait Selector[L <: Tuple, U]:
  def apply(t: L): U

given select[H, T <: Tuple]: Selector[H *: T, H] with
  def apply(t: H *: T) = t.head

given recurse[H, T <: Tuple, U](using selector: Selector[T, U]): Selector[H *: T, U] with
  def apply(t: H *: T) = selector(t.tail)

trait Modifier[T <: Tuple, U, V, W]:
  def apply(t: T, f: U => V): W

given modify1[T <: Tuple, U, V]: Modifier[U *: T, U, V, (U, V *: T)] with
  def apply(t: U *: T, f: U => V): (U, V *: T) =
    val u = t.head
    (u, f(u) *: t.tail)

given modify2[H, T <: Tuple, U, V, W <: Tuple](using modifier: Modifier[T, U, V, (U, W)]): Modifier[H *: T, U, V, (U, H *: W)] with
  def apply(t: H *: T, f: U => V): (U, H *: W) =
    val (u, out) = modifier(t.tail, f)
    (u, t.head *: out)

trait Remove[L <: Tuple, E, O]:
  def apply(t: L): O
  def reinsert(out: O): L

given remove[H, T <: Tuple]: Remove[H *: T, H, (H, T)] with
  def apply(t: H *: T): (H, T) = (t.head, t.tail)
  def reinsert(o: (H, T)): H *: T = o._1 *: o._2

given recursivelyRemove[H, T <: Tuple, E, O <: Tuple](using remover: Remove[T, E, (E, O)]): Remove[H *: T, E, (E, H *: O)] with
  def apply(t: H *: T): (E, H *: O) =
    val (e, tail) = remover(t.tail)
    (e, t.head *: tail)
  def reinsert(o: (E, H *: O)): H *: T =
    o._2.head *: remover.reinsert((o._1, o._2.tail))

trait NotContainsConstraint[L, U]
private class Instance[L, U]() extends NotContainsConstraint[L, U]
given emptyTupleNotContains[U]: NotContainsConstraint[EmptyTuple, U] = Instance[EmptyTuple, U]()
given tupleNotContains[H, T <: Tuple, U](using T NotContainsConstraint U)(using neq: scala.util.NotGiven[U =:= H]): NotContainsConstraint[H *: T, U] =
  Instance[H *: T, U]()

trait Union[L <: Tuple, M <: Tuple, O <: Tuple]:
  def apply(l: L, m: M): O

given tupleUnion[M <: Tuple]: Union[EmptyTuple, M, M] with
  def apply(l: EmptyTuple, m: M): M = m

given tupleUnion1[H, T <: Tuple, M <: Tuple, U <: Tuple]
  (using NotContainsConstraint[M, H])(using union: Union[T, M, U]): Union[H *: T, M, H *: U] with
  def apply(l: H *: T, m: M): H *: U = l.head *: union(l.tail, m)

given tupleUnion2[H, T <: Tuple, M <: Tuple, MR <: Tuple, U <: Tuple]
  (using remove: Remove[M, H, (H, MR)])(using union: Union[T, MR, U]): Union[H *: T, M, H *: U] with
  def apply(l: H *: T, m: M): H *: U = l.head *: union(l.tail, remove(m)._2)

trait Intersection[L <: Tuple, M <: Tuple, O <: Tuple]:
  def apply(l: L): O

given tupleIntersection[M <: Tuple]: Intersection[EmptyTuple, M, EmptyTuple] with
  def apply(l: EmptyTuple): EmptyTuple = EmptyTuple

given tupleIntersection1[H, T <: Tuple, M <: Tuple, I <: Tuple]
  (using ncc: NotContainsConstraint[M, H])(using intersection: Intersection[T, M, I]): Intersection[H *: T, M, I] with
  def apply(t: H *: T): I = intersection(t.tail)

given tupleIntersection2[H, T <: Tuple, M <: Tuple, MR <: Tuple, I <: Tuple]
  (using remove: Remove[M, H, (H, MR)])(using intersection: Intersection[T, MR, I]): Intersection[H *: T, M, H *: I] with
  def apply(t: H *: T): H *: I = t.head *: intersection(t.tail)

extension [T <: Tuple](t: T)
  def select[U](using selector: Selector[T, U]) = selector(t)
  def updateWith[U, V, W <: Tuple](f: U => V)(using replacer: Modifier[T, U, V, (U, W)]): W = replacer(t, f)._2
  def removeElem[U](using remove: Remove[T, U, _]) = remove(t)
  def union[U <: Tuple](u: U)(using union: Union[T, U, _]) = union(t, u)
  def intersect[U <: Tuple](using intersection: Intersection[T, U, _]) = intersection(t)