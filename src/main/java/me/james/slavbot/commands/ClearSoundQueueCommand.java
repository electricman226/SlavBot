package me.james.slavbot.commands;

import me.james.basebot.command.*;
import me.james.slavbot.*;
import sx.blah.discord.handle.obj.*;
import sx.blah.discord.util.audio.*;

public class ClearSoundQueueCommand extends Command
{

    @Override
    public String doCommand( String[] args, IUser user, IChannel chan, IMessage msg )
    {
        if ( user.getPermissionsForGuild( chan.getGuild() ).contains( Permissions.MANAGE_CHANNELS ) || user.getLongID() == SlavBot.BOT.getBot().getApplicationOwner().getLongID() )
        {
            AudioPlayer.getAudioPlayerForGuild( chan.getGuild() ).clear();
            return "Sound command queue cleared.";
        }
        return "This user cannot clear queue.";
    }
}
