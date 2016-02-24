package com.fortysevendeg.ninecardslauncher.process.user

import com.fortysevendeg.ninecardslauncher.commons.contexts.ContextSupport
import com.fortysevendeg.ninecardslauncher.commons.services.Service._
import com.fortysevendeg.ninecardslauncher.process.user.models.{User, Device}

trait UserProcess {

  def signIn(email: String, device: Device)(implicit context: ContextSupport): ServiceDef2[SignInResponse, UserException]

  def register(implicit context: ContextSupport): ServiceDef2[Unit, UserException]

  def unregister(implicit context: ContextSupport): ServiceDef2[Unit, UserException]

  def getUser(implicit context: ContextSupport): ServiceDef2[User, UserException]
}
