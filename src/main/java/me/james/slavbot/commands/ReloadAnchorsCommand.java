package me.james.slavbot.commands;

import me.james.basebot.command.*;
import me.james.slavbot.*;
import sx.blah.discord.handle.obj.*;
import sx.blah.discord.util.*;

public class ReloadAnchorsCommand extends Command
{
    @Override
    public String doCommand( String[] args, IUser user, IChannel chan, IMessage msg )
    {
        SlavBot.BOT.getLogger().info( String.format( "Reloading anchors... (told to by user %s (%s)", user.getName(), user.getStringID() ) );
        for ( Command c : SlavBot.anchorCmds )
            Command.removeCommand( c );
        SlavBot.registerAnchors();
        RequestBuffer.request( () -> {
            chan.sendMessage( "Reloaded all anchors." );
        } );
        SlavBot.BOT.getLogger().info( "Finished reloading anchors" );
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
