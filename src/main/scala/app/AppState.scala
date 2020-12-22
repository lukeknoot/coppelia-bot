package app

import zio.Ref
import sttp.model.CookieWithMeta
import zio.Has

case class State(cookies: Seq[CookieWithMeta], notifiedForVoucher: Boolean)

object AppState {

  type AppState = Has[Ref[State]]

  val live = Ref.make[State](State(Seq(), notifiedForVoucher = false)).toLayer

}
