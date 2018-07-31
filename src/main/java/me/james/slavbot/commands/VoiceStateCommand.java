package me.james.slavbot.commands;

import me.james.basebot.command.*;
import me.james.slavbot.*;
import sx.blah.discord.handle.obj.*;

public class VoiceStateCommand extends Command
{
    @Override
    public String doCommand( String[] args, IUser user, IChannel chan, IMessage msg )
    {
        if ( args.length < 2 )
            return null;

        IVoiceChannel targChan;
        try
        {
            targChan = SlavBot.BOT.getBot().getVoiceChannelByID( Long.parseLong( args[1] ) );
        }
        catch ( NumberFormatException e )
        {
            // ignore
            return null;
        }

        if ( targChan == null )
            return null;

        if ( targChan.isConnected() )
            targChan.leave();
        else
            targChan.join();

        return null;
    }

    @Override
    public boolean isPrivateMessageRequired()
    {
        return true;
    }

    @Override
    public boolean isBotOwnerSenderRequired()
    {
        return true;
    }
}
