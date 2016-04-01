package com.fortysevendeg.ninecardslauncher.app.ui.launcher.actions.publicollections

import com.fortysevendeg.ninecardslauncher.app.commons.Conversions
import com.fortysevendeg.ninecardslauncher.app.ui.commons.Presenter
import com.fortysevendeg.ninecardslauncher.app.ui.commons.TasksOps._
import com.fortysevendeg.ninecardslauncher.commons.services.Service._
import com.fortysevendeg.ninecardslauncher.process.collection.{CardException, CollectionException}
import com.fortysevendeg.ninecardslauncher.process.commons.models.Collection
import com.fortysevendeg.ninecardslauncher.process.commons.types.{Communication, NineCardCategory}
import com.fortysevendeg.ninecardslauncher.process.device.models.App
import com.fortysevendeg.ninecardslauncher.process.device.{AppException, GetByName}
import com.fortysevendeg.ninecardslauncher.process.sharedcollections.models.{SharedCollection, SharedCollectionPackage}
import com.fortysevendeg.ninecardslauncher.process.sharedcollections.{TopSharedCollection, SharedCollectionsExceptions, TypeSharedCollection}
import macroid.{ActivityContextWrapper, Ui}

import scalaz.concurrent.Task

class PublicCollectionsPresenter (actions: PublicCollectionsActions)(implicit contextWrapper: ActivityContextWrapper)
  extends Presenter
  with Conversions {

  protected var statuses = PublicCollectionStatuses(Communication, TopSharedCollection)

  def loadPublicCollectionsByCategory(category: NineCardCategory): Ui[Any] = {
    statuses = statuses.copy(category = category)
    loadPublicCollections()
  }

  def loadPublicCollectionsByTypeSharedCollection(typeSharedCollection: TypeSharedCollection): Ui[Any] = {
    statuses = statuses.copy(typeSharedCollection = typeSharedCollection)
    loadPublicCollections()
  }

  def loadPublicCollections(): Ui[Any] = Ui {
    Task.fork(getSharedCollections(statuses.category, statuses.typeSharedCollection).run).resolveAsyncUi(
      onPreTask = () => actions.showLoading(),
      onResult = (sharedCollections: Seq[SharedCollection]) => actions.loadPublicCollections(sharedCollections),
      onException = (ex: Throwable) => actions.showContactUsError())
  }

  def saveSharedCollection(sharedCollection: SharedCollection): Ui[Any] = Ui {
    Task.fork(addCollection(sharedCollection).run).resolveAsyncUi(
      onResult = (c) => actions.addCollection(c),
      onException = (ex) => actions.showContactUsError())
  }

  private[this] def getSharedCollections(
    category: NineCardCategory,
    typeSharedCollection: TypeSharedCollection): ServiceDef2[Seq[SharedCollection], SharedCollectionsExceptions] =
    di.sharedCollectionsProcess.getSharedCollectionsByCategory(category, typeSharedCollection)

  private[this] def addCollection(sharedCollection: SharedCollection):
  ServiceDef2[Collection, CollectionException with CardException with AppException] =
    for {
      collection <- di.collectionProcess.addCollection(toAddCollectionRequest(sharedCollection))
      appsInstalled <- di.deviceProcess.getSavedApps(GetByName)
      cards <- di.collectionProcess.addCards(collection.id, getCards(appsInstalled, sharedCollection.resolvedPackages))
    } yield collection.copy(cards = cards)

  private[this] def getCards(appsInstalled: Seq[App], packages: Seq[SharedCollectionPackage]) =
    packages map { pck =>
      appsInstalled find (_.packageName == pck.packageName) map { app =>
        toAddCollectionRequest(app)
      } getOrElse toAddCollectionRequest(pck)
    }

}

trait PublicCollectionsActions {

  def showLoading(): Ui[Any]

  def showContactUsError(): Ui[Any]

  def addCollection(collection: Collection): Ui[Any]

  def loadPublicCollections(sharedCollections: Seq[SharedCollection]): Ui[Any]

}

case class PublicCollectionStatuses(category: NineCardCategory, typeSharedCollection: TypeSharedCollection)