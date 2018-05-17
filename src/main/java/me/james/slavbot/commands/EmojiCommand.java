package me.james.slavbot.commands;

import me.james.basebot.command.*;
import sx.blah.discord.handle.obj.*;
import sx.blah.discord.util.*;

public class EmojiCommand extends Command
{
    @Override
    public String doCommand( String[] args, IUser user, IChannel chan, IMessage msg )
    {
        if ( args.length < 2 )
            return "Not enough args.";
        if ( msg.tokenize().hasNextEmoji() )
            RequestBuffer.request( () -> {
                chan.sendMessage( msg.tokenize().nextEmoji().getEmoji().getImageUrl() );
            } );
        return null;
    }
}
