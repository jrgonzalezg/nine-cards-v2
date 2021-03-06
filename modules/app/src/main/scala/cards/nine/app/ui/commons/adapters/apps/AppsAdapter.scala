/*
 * Copyright 2017 47 Degrees, LLC. <http://www.47deg.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cards.nine.app.ui.commons.adapters.apps

import java.io.Closeable

import android.support.v7.widget.{GridLayoutManager, RecyclerView}
import android.view.{LayoutInflater, View, ViewGroup}
import cards.nine.app.ui.commons.AsyncImageTweaks._
import cards.nine.app.ui.commons.UiContext
import cards.nine.app.ui.components.layouts.FastScrollerListener
import cards.nine.app.ui.components.widgets.ScrollingLinearLayoutManager
import cards.nine.app.ui.preferences.commons.{FontSize, IconsSize}
import cards.nine.models.types.theme.DrawerTextColor
import cards.nine.models.{
  ApplicationData,
  EmptyIterableApps,
  IterableApplicationData,
  NineCardsTheme
}
import com.fortysevendeg.ninecardslauncher.TypedResource._
import com.fortysevendeg.ninecardslauncher.{R, TR, TypedFindView}
import macroid.FullDsl._
import macroid._
import macroid.extras.ResourcesExtras._
import macroid.extras.TextViewTweaks._
import macroid.extras.ViewTweaks._

case class AppsAdapter(
    var apps: IterableApplicationData,
    clickListener: (ApplicationData) => Unit,
    longClickListener: Option[(View, ApplicationData) => Unit])(
    implicit val activityContext: ActivityContextWrapper,
    uiContext: UiContext[_],
    theme: NineCardsTheme)
    extends RecyclerView.Adapter[AppsIterableHolder]
    with FastScrollerListener
    with Closeable {

  val columnsLists = 4

  val heightItem = resGetDimensionPixelSize(R.dimen.height_app_item)

  override def getItemCount: Int = apps.count()

  override def onBindViewHolder(vh: AppsIterableHolder, position: Int): Unit =
    vh.bind(apps.moveToPosition(position)).run

  override def onCreateViewHolder(parent: ViewGroup, i: Int): AppsIterableHolder = {
    val view = LayoutInflater.from(parent.getContext).inflate(TR.layout.app_item, parent, false)
    AppsIterableHolder(view, clickListener, longClickListener)
  }

  def getLayoutManager: GridLayoutManager =
    new ScrollingLinearLayoutManager(columnsLists)

  def swapIterator(iter: IterableApplicationData) = {
    apps.close()
    apps = iter
    notifyDataSetChanged()
  }

  def clear() = {
    apps.close()
    apps = new EmptyIterableApps()
    notifyDataSetChanged()
  }

  override def close() = apps.close()

  override def getHeightAllRows = apps.count() / columnsLists * getHeightItem

  override def getHeightItem: Int = heightItem

  override def getColumns: Int = columnsLists
}

case class AppsIterableHolder(
    content: ViewGroup,
    clickListener: (ApplicationData) => Unit,
    longClickListener: Option[(View, ApplicationData) => Unit])(
    implicit context: ActivityContextWrapper,
    uiContext: UiContext[_],
    theme: NineCardsTheme)
    extends RecyclerView.ViewHolder(content)
    with TypedFindView {

  lazy val icon = Option(findView(TR.simple_item_icon))

  lazy val name = Option(findView(TR.simple_item_name))

  def bind(app: ApplicationData): Ui[_] =
    (icon <~ vResize(IconsSize.getIconApp) <~ ivSrcByPackageName(Some(app.packageName), app.name)) ~
      (name <~ tvSizeResource(FontSize.getSizeResource) <~ tvText(app.name) + tvColor(
        theme.get(DrawerTextColor))) ~
      (content <~
        On.click {
          Ui(clickListener(app))
        } <~
        (longClickListener map { listener =>
          FuncOn.longClick { view: View =>
            icon foreach (listener(_, app))
            Ui(true)
          }
        } getOrElse Tweak.blank))

  override def findViewById(id: Int): View = content.findViewById(id)
}
