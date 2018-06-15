package me.james.slavbot.commands;

import com.google.gson.*;
import java.io.*;
import java.util.*;
import me.james.basebot.command.*;
import me.james.slavbot.*;
import org.apache.commons.io.*;
import org.apache.http.*;
import org.apache.http.client.*;
import org.apache.http.client.methods.*;
import org.apache.http.impl.client.*;
import sx.blah.discord.handle.obj.*;
import sx.blah.discord.util.*;

public class RandomRedditCommand extends Command
{
    @Override
    public String doCommand( String[] args, IUser user, IChannel chan, IMessage msg )
    {
        String sub;
        JsonElement defSubElem;
        if ( args.length >= 2 )
            sub = args[1];
        else if ( ( defSubElem = SlavBot.BOT.getConfig( chan.getGuild() ).get( "defaultSubreddit" ) ) != null )
            sub = defSubElem.getAsString();
        else
            sub = "dankmemes";
        String url = "https://reddit.com/r/" + sub + ".json";
        try
        {
            HttpGet req = new HttpGet( url );
            HttpClient client = HttpClients.custom().setUserAgent( "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:15.0) Gecko/20100101 Firefox/15.0.1" ).build();
            HttpResponse resp = client.execute( req );
            String subJson = IOUtils.toString( resp.getEntity().getContent(), "UTF-8" );
            JsonObject json = new JsonParser().parse( subJson ).getAsJsonObject();
            JsonArray posts = json.get( "data" ).getAsJsonObject().get( "children" ).getAsJsonArray();
            if ( posts.size() <= 0 )
            {
                RequestBuffer.request( () -> {
                    chan.sendMessage( "This subreddit does not exist, or is completely empty." );
                } );
                return "Invalid subreddit";
            }
            JsonObject[] validPosts = getImagePosts( posts );
            if ( validPosts.length == 0 )
            {
                RequestBuffer.request( () -> {
                    chan.sendMessage( "This subreddit does not contain any top posts with images." );
                } );
                return "No url, validPosts len is 0";
            }
            JsonObject post = validPosts[new Random().nextInt( validPosts.length )];
            RequestBuffer.request( () -> {
                chan.sendMessage( "`" + post.get( "title" ).getAsString() + "`: " + post.get( "url" ).getAsString() );
            } );
        }
        catch ( IOException e )
        {
            e.printStackTrace();
        }
        return null;
    }

    private JsonObject[] getImagePosts( JsonArray posts )
    {
        ArrayList< JsonObject > validObjs = new ArrayList<>();
        posts.iterator().forEachRemaining( ( post ) -> {
            JsonObject obj = post.getAsJsonObject().get( "data" ).getAsJsonObject();
            if ( obj.has( "url" ) && ( obj.get( "url" ).getAsString().endsWith( ".png" ) || obj.get( "url" ).getAsString().endsWith( ".jpeg" ) || obj.get( "url" ).getAsString().endsWith( ".jpg" ) ) )
                validObjs.add( obj );
        } );
        return validObjs.toArray( new JsonObject[0] );
    }
}
