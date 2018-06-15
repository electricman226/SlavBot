package me.james.slavbot.commands.operator;

import java.util.*;
import me.james.slavbot.*;
import me.james.slavbot.commands.*;
import sx.blah.discord.handle.obj.*;
import sx.blah.discord.util.*;

public class TempMuteChannelCommand extends OperatorCommand
{
    public static HashMap< IChannel, Boolean > mutedChannel = new HashMap<>();

    @Override
    public String doOperatorCommand( String[] args, IUser user, IChannel chan, IMessage msg )
    {
        if ( args.length < 2 )
            return "Not enough args.";
        IChannel targetChan;
        if ( args[1].startsWith( "#" ) )
        {
            try
            {
                targetChan = SlavBot.BOT.getBot().getChannelByID( Long.parseLong( args[1].substring( 1 ) ) );
            }
            catch ( NumberFormatException e )
            {
                // ignore
                RequestBuffer.request( () -> {
                    chan.sendMessage( "Invalid channel ID." );
                } );
                return "Invalid channel ID (NumberFormatException).";
            }
        }
        else
        {
            List< IChannel > chans = chan.getGuild().getChannelsByName( args[1] );
            if ( chans.size() <= 0 )
            {
                RequestBuffer.request( () -> {
                    chan.sendMessage( "Invalid channel." );
                } );
                return "Invalid channel (chans len is 0)";
            }
            targetChan = chans.get( 0 );
        }
        if ( targetChan == null )
        {
            RequestBuffer.request( () -> {
                chan.sendMessage( "Invalid channel." );
            } );
            return "Invalid channel (targetChan is NULL)";
        }

        boolean chanMuteStatus = !mutedChannel.getOrDefault( targetChan, false );
        IRole everyone = targetChan.getGuild().getEveryoneRole();
        IRole operator = getOperatorRole( targetChan.getGuild() );
        if ( chanMuteStatus )
        {
            targetChan.overrideRolePermissions( everyone, null, EnumSet.of( Permissions.SEND_MESSAGES ) );
            targetChan.overrideRolePermissions( operator, EnumSet.of( Permissions.READ_MESSAGES, Permissions.READ_MESSAGE_HISTORY, Permissions.ADD_REACTIONS, Permissions.SEND_MESSAGES ), null );
        }
        else
        {
            targetChan.removePermissionsOverride( everyone );
        }
        mutedChannel.put( targetChan, chanMuteStatus );
        return "Toggled channel mute. (" + chanMuteStatus + ")";
    }
}
