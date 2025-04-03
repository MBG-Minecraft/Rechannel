# Discord Bot REChannel *(record-channel)*

A Discord bot that records every users' voice from a voice channel to a seperate audio track.

### Building
Building jar with `gradlew clean shadowJar` or see releases https://github.com/Team-MrBeast/Rechannel/releases
<br>
Or running with `gradlew clean runShadow`

`java -jar mrbeast-rechannel-1.0.jar` optional arguments include `--resetConfig` and `--debug`

### Installation
<img width="1098" alt="Untitled" src="https://github.com/user-attachments/assets/51e9f07c-7ede-4e4c-a4a2-2990f4e97b39" />
Due to the current way Discord applies applications to guilds, you have to enable the guild installation checkmark in installation.
Grant the authorization URL, view channel, send messages and connect permissions from the drop down menu on the same page.
<br><br>
Then go to the authorization URL with an associated Discord account logged in, and select the guild to add the bot application to.
Once added, remove the guild invite checkmark to disallow the public to add the bot to their servers.
<br><br>
Next create a .env in the same directory as the .jar file, and add line `DISCORD_TOKEN=INSERT_TOKEN` replace `INSERT_TOKEN` with your bot token.

### Commands
`/record <channel> [<segmentedSeconds>] [<volume>]`
- Starts recording activity in a voice channel.
>- segmentedSeconds is how often to dump the audio track segements to file. Default is every 15 minutes.
>- volume is a value between 0.0 and 1.0 for how loud to record the audio at. Default is 1.0

`/stoprecording`
- Stops recording in any voice channel from the guild where the command was ran.

`/follow <user> [<segmentedSeconds>] [<volume>]`
- Follows a user around in their voice channel and records their audio.
- _Everytime they swap channels, the bot has to flush the buffer to file._
>- segmentedSeconds is how often to dump the audio track segements to file. Default is every 15 minutes.
>- volume is a value between 0.0 and 1.0 for how loud to record the audio at. Default is 1.0
<br><br>

`/list <user>`
- Lists all recordings directories for a user.

`/get <user> <date>`
- Downloads a recording .zip from the yyyy-mm-dd date and sends it to the command sender.

Recorded .mp3 files are located at `/recordings/$user/$date/`
<br>
There are two formats, withSilence and rawNoSilence.
<br>
Discord provides the packets as is, with no silence added.
<br>
So to make synchronization easier for editors, silence is added at points where the user doesn't talk.
<br>
This makes all audio tracks the same length for that recording segment.
