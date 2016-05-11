package com.fortysevendeg.ninecardslauncher.app.ui.components.layouts

import android.content.Context
import android.util.AttributeSet
import android.view.DragEvent._
import android.view.View.OnDragListener
import android.view.{DragEvent, LayoutInflater, View}
import android.widget.LinearLayout
import com.fortysevendeg.macroid.extras.TextTweaks._
import com.fortysevendeg.ninecardslauncher.app.ui.commons.CommonsTweak._
import com.fortysevendeg.ninecardslauncher.app.ui.commons.DragObject
import com.fortysevendeg.ninecardslauncher.app.ui.commons.ViewOps._
import com.fortysevendeg.ninecardslauncher.app.ui.components.widgets.TintableButton
import com.fortysevendeg.ninecardslauncher.app.ui.components.widgets.tweaks.TintableButtonTweaks._
import com.fortysevendeg.ninecardslauncher.app.ui.launcher.LauncherPresenter
import com.fortysevendeg.ninecardslauncher.app.ui.launcher.types.ReorderCollection
import com.fortysevendeg.ninecardslauncher.commons.javaNull
import com.fortysevendeg.ninecardslauncher.process.theme.models.{NineCardsTheme, PrimaryColor}
import com.fortysevendeg.ninecardslauncher2.{R, TR, TypedFindView}
import macroid._

class CollectionActionsPanelLayout(context: Context, attrs: AttributeSet, defStyle: Int)
  extends LinearLayout(context, attrs, defStyle)
  with TypedFindView {

  def this(context: Context) = this(context, javaNull, 0)

  def this(context: Context, attrs: AttributeSet) = this(context, attrs, 0)

  val unselectedPosition = -1

  val selectedScale = 1.1f

  val defaultScale = 1f

  LayoutInflater.from(context).inflate(R.layout.collections_actions_view_panel, this)

  var actions: Seq[CollectionActionItem] = Seq.empty

  var draggingTo: Option[Int] = None

  def load(actions: Seq[CollectionActionItem])
    (implicit theme: NineCardsTheme, presenter: LauncherPresenter, contextWrapper: ActivityContextWrapper): Ui[Any] = {
    this.actions = actions
    Ui.sequence(actions.zipWithIndex map {
      case (action, index) => buttonByIndex(index) <~ populate(action, index)
    }: _*)
  }

  def dragAddItemController(action: Int, x: Float, y: Float)(implicit presenter: LauncherPresenter, contextWrapper: ActivityContextWrapper): Unit = {
    android.util.Log.d("9cards", s"action: $action ($x, $y)")
    action match {
      case ACTION_DRAG_LOCATION =>
        val newPosition = Some(calculatePosition(x))
        if (newPosition != draggingTo) {
          draggingTo = newPosition
          (this <~ (draggingTo map select getOrElse select(unselectedPosition))).run
        }
      case ACTION_DROP =>
        draggingTo flatMap actions.lift map { action =>
          action.collectionActionType match {
            case CollectionActionAppInfo => presenter.settingsInAddItem()
            case CollectionActionUninstall => presenter.uninstallInAddItem()
            case _ =>
          }
        } getOrElse {
          presenter.endAddItem()
        }
        draggingTo = None
        (this <~ select(unselectedPosition)).run
      case ACTION_DRAG_EXITED =>
        draggingTo = None
        (this <~ select(unselectedPosition)).run
      case ACTION_DRAG_ENDED =>
        presenter.endAddItem()
        draggingTo = None
        (this <~ select(unselectedPosition)).run
      case _ =>
    }
  }

  private[this] def calculatePosition(x: Float): Int = x.toInt / (getWidth / actions.length)

  private[this] def populate(action: CollectionActionItem, position: Int)
    (implicit theme: NineCardsTheme, presenter: LauncherPresenter, contextWrapper: ActivityContextWrapper): Tweak[TintableButton] =
    tvText(action.name) +
      tvCompoundDrawablesWithIntrinsicBoundsResources(left = action.resource) +
      vSetPosition(position) +
      dragListenerStyle(action.collectionActionType) +
      tbPressedColor(theme.get(PrimaryColor)) +
      tbResetColor

  private[this] def select(position: Int)(implicit contextWrapper: ActivityContextWrapper) = Transformer {
    case view: TintableButton if view.getPosition.contains(position) => Ui(view.setPressedColor())
    case view: TintableButton => Ui(view.setDefaultColor())
  }

  private[this] def buttonByIndex(index: Int): Option[TintableButton] = index match {
    case 0 => Option(findView(TR.launcher_collections_action_1))
    case 1 => Option(findView(TR.launcher_collections_action_2))
    case _ => None
  }

  private[this] def dragListenerStyle(collectionActionType: CollectionActionType)(implicit presenter: LauncherPresenter): Tweak[View] = Tweak[View] { view =>
    view.setOnDragListener(new OnDragListener {
      override def onDrag(v: View, event: DragEvent): Boolean = {
        event.getLocalState match {
          case DragObject(_, ReorderCollection) =>
            (event.getAction, v, collectionActionType) match {
              case (ACTION_DRAG_ENTERED, tb: TintableButton, _) => tb.setPressedColor()
              case (ACTION_DRAG_EXITED, tb: TintableButton, _) => tb.setDefaultColor()
              case (ACTION_DROP, _, CollectionActionRemove) => presenter.removeCollectionInReorderMode()
              case (ACTION_DROP, _, CollectionActionEdit) => presenter.editCollectionInReorderMode()
              case _ =>
            }
            true
          case _ => false
        }
      }
    })
  }

}

case class CollectionActionItem(name: String, resource: Int, collectionActionType: CollectionActionType)

sealed trait CollectionActionType

case object CollectionActionAppInfo extends CollectionActionType

case object CollectionActionUninstall extends CollectionActionType

case object CollectionActionRemove extends CollectionActionType

case object CollectionActionEdit extends CollectionActionType

