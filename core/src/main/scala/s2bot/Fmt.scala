package s2bot

import slack.models.{Channel, User}

object Fmt {

  def linkMessageUrl(bot: S2Bot, channelId: String, ts: String): String = s"https://${bot.rtmState.team.domain}.slack.com/archives/$channelId/p${ts.replaceAll("\\.", "")}"

  def linkChannel(channelId: String): String = s"<#$channelId>"

  def linkChannel(channel: Channel): String = linkChannel(channel.id)

  def linkChannelForName(bot: S2Bot, channelName: String): String = bot.getChannelIdForName(channelName).map(linkChannel).getOrElse(s"#$channelName")

  def linkUser(userId: String): String = s"<@$userId>"

  def linkUser(user: User): String = linkUser(user.id)

  def linkUserForName(bot: S2Bot, userName: String): String = bot.getChannelIdForName(userName).map(linkUser).getOrElse(s"@$userName")

}
