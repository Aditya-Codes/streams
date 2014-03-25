package org.apache.streams.twitter.serializer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.apache.streams.data.ActivitySerializer;
import org.apache.streams.exceptions.ActivitySerializerException;
import org.apache.streams.pojo.json.Activity;
import org.apache.streams.pojo.json.ActivityObject;
import org.apache.streams.pojo.json.Actor;
import org.apache.streams.twitter.Url;
import org.apache.streams.twitter.pojo.Retweet;
import org.apache.streams.twitter.pojo.Tweet;
import org.apache.streams.twitter.pojo.User;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.streams.data.util.ActivityUtil.ensureExtensions;

/**
* Created with IntelliJ IDEA.
* User: mdelaet
* Date: 9/30/13
* Time: 9:24 AM
* To change this template use File | Settings | File Templates.
*/
public class TwitterJsonRetweetActivitySerializer extends TwitterJsonEventActivitySerializer implements ActivitySerializer<String> {

    public Activity convert(ObjectNode event) throws ActivitySerializerException {

        Retweet retweet = null;
        try {
            retweet = mapper.treeToValue(event, Retweet.class);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        Activity activity = new Activity();
        activity.setActor(buildActor(retweet));
        activity.setVerb("share");
        activity.setObject(buildActivityObject(retweet.getRetweetedStatus()));
        activity.setId(formatId(activity.getVerb(), retweet.getIdStr()));
        if(Strings.isNullOrEmpty(activity.getId()))
            throw new ActivitySerializerException("Unable to determine activity id");
        try {
            activity.setPublished(parse(retweet.getCreatedAt()));
        } catch( Exception e ) {
            throw new ActivitySerializerException("Unable to determine publishedDate", e);
        }
        activity.setGenerator(buildGenerator(event));
        activity.setIcon(getIcon(event));
        activity.setProvider(buildProvider(event));
        activity.setTitle("");
        activity.setContent(retweet.getRetweetedStatus().getText());
        activity.setUrl(getUrls(event));
        activity.setLinks(getLinks(retweet));
        addTwitterExtension(activity, event);
        addLocationExtension(activity, retweet);
        return activity;
    }

    public static Actor buildActor(Tweet tweet) {
        Actor actor = new Actor();
        User user = tweet.getUser();
        actor.setId(formatId(user.getIdStr(), tweet.getIdStr()));
        actor.setDisplayName(user.getScreenName());
        actor.setId(user.getIdStr());
        if (user.getUrl()!=null){
            actor.setUrl(user.getUrl());
        }
        return actor;
    }

    public static ActivityObject buildActivityObject(Tweet tweet) {
        ActivityObject actObj = new ActivityObject();
        actObj.setId(formatId(tweet.getIdStr()));
        actObj.setObjectType("tweet");
        return actObj;
    }

    public static List<Object> getLinks(Retweet retweet) {
        List<Object> links = Lists.newArrayList();
        for( Url url : retweet.getRetweetedStatus().getEntities().getUrls() ) {
            links.add(url.getExpandedUrl());
        }
        return links;
    }

    public static void addLocationExtension(Activity activity, Retweet retweet) {
        Map<String, Object> extensions = ensureExtensions(activity);
        Map<String, Object> location = new HashMap<String, Object>();
        location.put("id", formatId(retweet.getIdStr()));
        location.put("coordinates", retweet.getCoordinates());
        extensions.put("location", location);
    }

}
