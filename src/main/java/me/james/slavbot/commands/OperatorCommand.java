package me.james.slavbot.commands;

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
        if ( !SlavBot.BOT.getConfig( chan.getGuild() ).has( "operatorRole" ) )
            return "No operator role? (is null)";
        long operatorRoleId = SlavBot.BOT.getConfig( chan.getGuild() ).get( "operatorRole" ).getAsLong();

        if ( user.getRolesForGuild( chan.getGuild() ).stream().anyMatch( role -> role.getLongID() == operatorRoleId ) )
            return doOperatorCommand( args, user, chan, msg );

        return "User was not an operator.";
    }

    public abstract String doOperatorCommand( String[] args, IUser user, IChannel chan, IMessage msg );
}
