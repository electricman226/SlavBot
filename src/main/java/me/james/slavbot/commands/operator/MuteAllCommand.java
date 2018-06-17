package me.james.slavbot.commands.operator;

import java.util.*;
import me.james.slavbot.*;
import sx.blah.discord.handle.obj.*;
import sx.blah.discord.util.*;

public class MuteAllCommand extends OperatorCommand
{
    public static HashMap< IGuild, Boolean > mutedServers = new HashMap<>();

    @Override
    public String doOperatorCommand( String[] args, IUser user, IChannel chan, IMessage msg )
    {
        IVoiceChannel vc;
        if ( ( vc = user.getVoiceStateForGuild( chan.getGuild() ).getChannel() ) == null )
            return null;
        boolean muteStatus = !mutedServers.getOrDefault( chan.getGuild(), false );
        for ( IUser chanUser : vc.getConnectedUsers() )
            if ( chanUser != user && chanUser != SlavBot.BOT.getBot().getApplicationOwner() && user.getLongID() != vc.getGuild().getOwnerLongID() )
                RequestBuffer.request( () -> chan.getGuild().setMuteUser( chanUser, muteStatus ) );
        mutedServers.put( chan.getGuild(), muteStatus );
        return "Toggled voice mute. (" + muteStatus + ")";
    }
}
