package me.james.slavbot.commands;

import com.google.gson.*;
import java.util.*;
import me.james.basebot.command.*;
import me.james.slavbot.*;
import sx.blah.discord.handle.obj.*;

public abstract class OperatorCommand extends Command
{
    @Override
    public String doCommand( String[] args, IUser user, IChannel chan, IMessage msg )
    {
        if ( !SlavBot.BOT.getConfig( chan.getGuild() ).has( "operatorChannel" ) || chan.getLongID() != SlavBot.BOT.getConfig( chan.getGuild() ).get( "operatorChannel" ).getAsLong() )
            return "Not an operator channel.";
        if ( !SlavBot.BOT.getConfig( chan.getGuild() ).has( "operators" ) )
            return "No operators? (jarray is null)";
        JsonArray operators = SlavBot.BOT.getConfig( chan.getGuild() ).get( "operators" ).getAsJsonArray();
        if ( operators.size() <= 0 )
            return "No operators? (len is 0)";
        List< IRole > roles = user.getRolesForGuild( chan.getGuild() );

        for ( JsonElement elem : operators )
            if ( roles.stream().anyMatch( role -> role.getLongID() == elem.getAsLong() ) )
                return doOperatorCommand( args, user, chan, msg );

        return "User was not an operator.";
    }

    public abstract String doOperatorCommand( String[] args, IUser user, IChannel chan, IMessage msg );
}
