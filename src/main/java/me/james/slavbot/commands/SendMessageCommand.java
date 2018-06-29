package me.james.slavbot.commands;

import me.james.basebot.command.*;
import me.james.slavbot.*;
import sx.blah.discord.handle.obj.*;

public class SendMessageCommand extends Command
{
    @Override
    public String doCommand( String[] args, IUser user, IChannel chan, IMessage msg )
    {
        if ( args.length < 3 )
            return "Not enough args.";
        try
        {
            IChannel targetChan = SlavBot.BOT.getBot().getChannelByID( Long.parseLong( args[1] ) );
            StringBuilder builder = new StringBuilder();
            for ( int i = 2; i < args.length; i++ )
                builder.append( args[i] ).append( " " );
            targetChan.sendMessage( builder.toString().trim() );
        }
        catch ( NumberFormatException e )
        {
            chan.sendMessage( "Invalid channel ID." );
            return "Invalid channel ID.";
        }
        return null;
    }

    @Override
    public boolean isBotOwnerSenderRequired()
    {
        return true;
    }

    @Override
    public boolean isPrivateMessageRequired()
    {
        return true;
    }
}
