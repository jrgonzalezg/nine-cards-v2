package com.fortysevendeg.ninecardslauncher.app.ui.profile

import android.app.Activity
import android.os.Bundle
import android.support.design.widget.AppBarLayout
import android.support.v7.app.AppCompatActivity
import android.view.{MenuItem, Menu}
import com.fortysevendeg.ninecardslauncher.app.commons.ContextSupportProvider
import com.fortysevendeg.ninecardslauncher.app.di.Injector
import com.fortysevendeg.ninecardslauncher.app.ui.commons.AppUtils._
import com.fortysevendeg.ninecardslauncher.app.ui.commons.{GoogleApiClientProvider, ActivityUiContext, UiContext, SystemBarsTint}
import com.fortysevendeg.ninecardslauncher.app.ui.commons.TasksOps._
import com.fortysevendeg.ninecardslauncher.process.theme.models.NineCardsTheme
import com.fortysevendeg.ninecardslauncher2.{R, TypedFindView}
import com.google.android.gms.common.api.GoogleApiClient
import macroid.{Ui, Contexts}
import macroid.FullDsl._
import rapture.core.Answer

import scala.util.Try
import scalaz.concurrent.Task

case class GoogleApiClientStatuses(
  apiClient: Option[GoogleApiClient] = None,
  username: Option[String] = None)

class ProfileActivity
  extends AppCompatActivity
  with Contexts[AppCompatActivity]
  with ContextSupportProvider
  with TypedFindView
  with SystemBarsTint
  with ProfileTabListener
  with ProfileComposer
  with ProfileTasks
  with GoogleApiClientProvider
  with AppBarLayout.OnOffsetChangedListener {

  implicit lazy val di = new Injector

  implicit lazy val uiContext: UiContext[Activity] = ActivityUiContext(this)

  implicit lazy val theme: NineCardsTheme = di.themeProcess.getSelectedTheme.run.run match {
    case Answer(t) => t
    case _ => getDefaultTheme
  }

  var clientStatuses = GoogleApiClientStatuses()

  override def onCreate(bundle: Bundle) = {
    super.onCreate(bundle)
    loadUserInfo
    setContentView(R.layout.profile_activity)
    runUi(initUi)

    toolbar foreach setSupportActionBar
    Option(getSupportActionBar) foreach { actionBar =>
      actionBar.setDisplayHomeAsUpEnabled(true)
      actionBar.setHomeAsUpIndicator(iconIndicatorDrawable)
    }

    barLayout foreach (_.addOnOffsetChangedListener(this))
  }

  override def onStop(): Unit = {
    clientStatuses match {
      case GoogleApiClientStatuses(Some(client), _) => Try(client.disconnect())
      case _ =>
    }
    super.onStop()
  }

  override def onCreateOptionsMenu(menu: Menu): Boolean = {
    getMenuInflater.inflate(R.menu.profile_menu, menu)
    super.onCreateOptionsMenu(menu)
  }

  override def onOptionsItemSelected(item: MenuItem): Boolean = item.getItemId match {
    case android.R.id.home =>
      finish()
      true
    case _ =>
      super.onOptionsItemSelected(item)
  }

  override def onOffsetChanged(appBarLayout: AppBarLayout, offset: Int): Unit = {
    val maxScroll = appBarLayout.getTotalScrollRange.toFloat
    val percentage = Math.abs(offset) / maxScroll

    runUi(handleToolbarVisibility(percentage) ~ handleProfileVisibility(percentage))
  }

  override def onRequestConnectionError(errorCode: Int): Unit =
    showError(R.string.errorConnectingGoogle, () => tryToConnect())

  override def onResolveConnectionError(): Unit =
    showError(R.string.errorConnectingGoogle, () => tryToConnect())

  override def tryToConnect(): Unit = clientStatuses.apiClient foreach (_.connect())

  override def onConnected(bundle: Bundle): Unit = {
    super.onConnected(bundle)
    clientStatuses match {
      case GoogleApiClientStatuses(Some(client), Some(email)) if client.isConnected =>
        loadUserAccounts(client, email)
      case _ => showError(R.string.errorConnectingGoogle, () => tryToConnect())
    }
  }

  def sampleItems(tab: String) = 1 to 20 map (i => s"$tab Item $i")

  override def onProfileTabSelected(profileTab: ProfileTab): Unit = profileTab match {
    case PublicationsTab =>
      // TODO - Load publications and set adapter
      runUi(setPublicationsAdapter(sampleItems("Publication")))
    case SubscriptionsTab =>
      // TODO - Load subscriptions and set adapter
      runUi(setSubscriptionsAdapter(sampleItems("Subscription")))
    case AccountsTab =>
      clientStatuses match {
        case GoogleApiClientStatuses(Some(client), Some(email)) if client.isConnected =>
          loadUserAccounts(client, email)
        case GoogleApiClientStatuses(Some(client), Some(email)) =>
          tryToConnect()
          runUi(showLoading)
        case _ =>
          loadUserEmail()
      }
  }

  private[this] def loadUserInfo(implicit uiContext: UiContext[_]): Unit =
    Task.fork(di.userConfigProcess.getUserInfo.run).resolveAsyncUi(
      onResult = userInfo => userProfile(userInfo.email, userInfo.imageUrl)
    )

  private[this] def loadUserEmail(): Unit =
    Task.fork(loadSingedEmail.run).resolveAsyncUi(
      onResult = email => Ui {
        val client = createGoogleDriveClient(email)
        clientStatuses = clientStatuses.copy(
          apiClient = Some(client),
          username = Some(email))
        client.connect()
      },
      onException = (_) => showError(R.string.errorLoadingUser, loadUserEmail),
      onPreTask = () => showLoading
    )

  private[this] def loadUserAccounts(client: GoogleApiClient, username: String): Unit =
    Task.fork(loadAccounts(client, username).run).resolveAsyncUi(
      onResult = accountSyncs => setAccountsAdapter(accountSyncs),
      onException = (_) => showError(R.string.errorConnectingGoogle, () => loadUserAccounts(client, username)),
      onPreTask = () => showLoading
    )

}