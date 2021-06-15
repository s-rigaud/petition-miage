package miage;

import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ThreadLocalRandom;

import com.google.api.server.spi.auth.common.User;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.CompositeFilterOperator;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.datastore.QueryResultList;

public class Utils {

	public static int getRandomValue(int min, int max) {
		return ThreadLocalRandom.current().nextInt(min, max + 1);
	}

	/*
	 * Ugly and nasty way of returning petition key corresponding for a bucket " in
	 * petition name make the thing crash TODO fix this
	 */
	public static String extractKey(Entity petitionBucket) {
		String key = (String) petitionBucket.getKey().getName();
		return key.split("\"")[1];
	}

	public static Entity createPetition(DatastoreService datastore, String petitionName, String petitionBody,
			String ownerMail, ArrayList<String> tags, boolean isTest, String separator) {

		// TODO add fake sign
		// TODO validate tags with PetitionEndpoints.AVAILABLE_TAGS
		Entity petition = new Entity("Petition", Long.MAX_VALUE - (new Date()).getTime() + separator + ownerMail);

		if (isTest) { // Tags will be integers between 1 and 9
			for (int i = 0; i < Utils.getRandomValue(0, 10); i++) {
				tags.add(String.valueOf(i));
			}
		}

		petition.setProperty("name", petitionName);
		petition.setProperty("owner", ownerMail);
		petition.setProperty("created_at", new Date());
		petition.setProperty("signCount", 0);
		petition.setProperty("content", petitionBody);
		petition.setProperty("tags", tags);
		datastore.put(petition);

		Entity petitionBucket;
		// Create 10 empty buckets for each petition
		// Bucket can contains up to 40,000 signs

		for (int j = 0; j < 10; j++) {
			petitionBucket = new Entity("PetitionBucket", petition.getKey() + separator + j);
			// Datatable doesn't make the difference between null and empty list/ string
			petitionBucket.setProperty("voters", new ArrayList<String>());
			datastore.put(petitionBucket);
		}
		return petition;
	}

	/*
	 * Retrieve the 10 buckets of a specific petition and return if user's vote is
	 * already in one of them
	 */
	/* Response is O(10) */
	public static boolean doesUserAlreadySignPetition(User user, Key petitionKey, String separator) {
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		Query q = new Query("PetitionBucket").setFilter(CompositeFilterOperator.and(
				new FilterPredicate("__key__", FilterOperator.GREATER_THAN_OR_EQUAL,
						KeyFactory.createKey("PetitionBucket", petitionKey + separator + "0")),
				new FilterPredicate("__key__", FilterOperator.LESS_THAN,
						// : is just after 9 in ASCII order
						KeyFactory.createKey("PetitionBucket", petitionKey + separator + ":")),
				new FilterPredicate("voters", FilterOperator.EQUAL, user.getEmail())));

		PreparedQuery pq = datastore.prepare(q);
		FetchOptions fetchOptions = FetchOptions.Builder.withLimit(10);
		QueryResultList<Entity> signedPetitionBucket = pq.asQueryResultList(fetchOptions);
		return signedPetitionBucket.size() != 0;

	}
}
