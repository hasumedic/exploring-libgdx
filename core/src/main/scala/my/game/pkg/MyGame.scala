package my.game.pkg

import akka.actor.{ActorRef, ActorSystem}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.stream.scaladsl.{Flow, Sink, Source}
import com.badlogic.gdx.graphics.g2d.{BitmapFont, SpriteBatch}
import com.badlogic.gdx.graphics.{GL20, OrthographicCamera}
import com.badlogic.gdx.{Game, Gdx, ScreenAdapter}

class MyGame extends Game {

  override def create(): Unit = {
    setScreen(new MainScreen)
  }

}

class MainScreen extends ScreenAdapter {

  private lazy val camera = new OrthographicCamera()
  private val batch: SpriteBatch = new SpriteBatch()

  var tick = 1L

  implicit val actorSystem: ActorSystem = ActorSystem("game")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val tickSource: Source[TickDelta, ActorRef] = Source.actorRef[TickDelta](0, OverflowStrategy.dropNew)
  var tickActor: Option[ActorRef] = Option.empty[ActorRef]

  private lazy val font = {
    val f = new BitmapFont()
    f.getData.setScale(2f)
    f
  }

  override def show(): Unit = {
    camera.setToOrtho(false, 800, 480)

    val tickSettingFlow = Flow[TickDelta].map { tickDelta =>
      tick += 1
      tickDelta
    }

    val graph = tickSource.via(tickSettingFlow).to(Sink.ignore)

    tickActor = Some(graph.run())
  }

  override def render(delta: Float): Unit = {
    tickActor.foreach(_ ! delta)

    Gdx.gl.glClearColor(0, 0, 0.5f, 1)
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
    camera.update()
    batch.setProjectionMatrix(camera.combined)
    batch.begin()
    font.draw(batch, s"Tick: $tick", 50, font.getCapHeight)
    batch.end()
  }

  override def dispose(): Unit = {
    actorSystem.terminate()
    ()
  }

}
