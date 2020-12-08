package app

import zio.Ref
import sttp.model.CookieWithMeta
import zio.Has

object AppState {

  type AppState = Has[Ref[Seq[CookieWithMeta]]]

  val live = Ref.make[Seq[CookieWithMeta]](Seq()).toLayer

}
