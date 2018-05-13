package me.james.slavbot.commands;

import me.james.basebot.command.*;
import me.james.slavbot.*;
import sx.blah.discord.handle.obj.*;

public class ListSoundsCommand extends Command
{
    @Override
    public String doCommand( String[] args, IUser user, IChannel chan, IMessage msg )
    {
        StringBuilder builder = new StringBuilder( "The following sound commands exist:\n" );
        for ( String s : SlavBot.soundList )
            builder.append( "- " ).append( "`" ).append( s ).append( "`" ).append( "\n" );
        chan.sendMessage( builder.toString() );
        return null;
    }
}
