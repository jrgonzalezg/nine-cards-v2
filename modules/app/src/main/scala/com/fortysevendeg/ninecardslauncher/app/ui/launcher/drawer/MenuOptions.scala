package com.fortysevendeg.ninecardslauncher.app.ui.launcher.drawer

import AppsAlphabeticalNames._
import ContactsMenuOptionNames._

sealed trait AppsMenuOption {
  val name: String
}

case object AppsAlphabetical extends AppsMenuOption {
  override val name: String = appsAlphabetical
}

case object AppsByCategories extends AppsMenuOption {
  override val name: String = appsByCategories
}

case object AppsByLastInstall extends AppsMenuOption {
  override val name: String = appsByLastInstall
}

object AppsAlphabeticalNames {
  val appsAlphabetical = "AppsAlphabetical"
  val appsByCategories = "AppsByCategories"
  val appsByLastInstall = "AppsByLastInstall"
}

sealed trait ContactsMenuOption {
  val name: String
}

case object ContactsAlphabetical extends ContactsMenuOption {
  override val name: String = contactsAlphabetical
}

case object ContactsFavorites extends ContactsMenuOption {
  override val name: String = contactsFavorites
}

case object ContactsByLastCall extends ContactsMenuOption {
  override val name: String = contactsByLastCall
}

object ContactsMenuOptionNames {
  val contactsAlphabetical = "ContactsAlphabetical"
  val contactsFavorites = "ContactsFavorites"
  val contactsByLastCall = "ContactsByLastCall"
}