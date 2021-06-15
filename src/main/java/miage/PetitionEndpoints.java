package miage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.github.javafaker.Faker;
import com.google.api.server.spi.auth.common.User;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.config.Named;
import com.google.api.server.spi.config.Nullable;
import com.google.api.server.spi.response.CollectionResponse;
import com.google.api.server.spi.response.UnauthorizedException;
import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.CompositeFilterOperator;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.appengine.api.datastore.QueryResultList;

@Api(name = "petitionapi", version = "v1", //
		audiences = "783652474514-hsrkuk75ikl453pu5fq2nf0m43q3qcsi.apps.googleusercontent.com", //
		clientIds = "783652474514-hsrkuk75ikl453pu5fq2nf0m43q3qcsi.apps.googleusercontent.com", //
		namespace = @ApiNamespace(//
				ownerDomain = "pitchounous.miage.com", //
				ownerName = "Les pitchounous", //
				packagePath = "") //
)
public class PetitionEndpoints {

	public final String[] AVAILABLE_TAGS = { "cookie", "milk", "cafe" };
	private final String SEP = "_";

	@ApiMethod(name = "createFakePetitions", httpMethod = HttpMethod.GET, path = "/petitions/create/fake")
	public List<Entity> createFakePetitions() {

		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		Faker faker = new Faker(new Locale("fr"));
		Entity petition;

		List<String> randomMails = new ArrayList<String>();
		for (int i = 0; i < 10; i++) {
			randomMails.add(faker.internet().emailAddress());
		}

		// Create petitions
		List<Entity> petitions = new ArrayList<Entity>();
		for (int i = 0; i < 10; i++) {
			String owner = randomMails.get(Utils.getRandomValue(0, 9));
			String petitionName = faker.address().city() + " est la meilleure ville";
			petition = Utils.createPetition(datastore, petitionName, petitionName + ". Oh oui !", owner,
					new ArrayList<String>(), true, SEP);
			petitions.add(petition);
		}
		return petitions;
	}

	/* Connection not required endpoints ↓ */
	@ApiMethod(name = "topPetitions", httpMethod = HttpMethod.GET, path = "/petitions/top100")
	public CollectionResponse<Entity> topPetitions(User user, @Nullable @Named("next") String cursorString) {

		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		Query q = new Query("Petition").addSort("signCount", SortDirection.DESCENDING).addSort("created_at",
				SortDirection.DESCENDING);
		PreparedQuery pq = datastore.prepare(q);
		FetchOptions fetchOptions = FetchOptions.Builder.withLimit(10);

		if (cursorString != null) {
			fetchOptions.startCursor(Cursor.fromWebSafeString(cursorString));
		}

		QueryResultList<Entity> petitions = pq.asQueryResultList(fetchOptions);
		cursorString = petitions.getCursor().toWebSafeString();

		for (Entity p : petitions) {
			p.setProperty("userAlreadySigned",
					user != null && Utils.doesUserAlreadySignPetition(user, p.getKey(), SEP));
		}

		return CollectionResponse.<Entity>builder().setItems(petitions).setNextPageToken(cursorString).build();

	}

	@ApiMethod(name = "getSinglePetition", httpMethod = HttpMethod.GET, path = "/petition")
	public Entity getSinglePetition(User user, @Named("id") String key) throws EntityNotFoundException {
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		Entity petition = null;
		try {
			petition = datastore.get(KeyFactory.createKey("Petition", key));
			petition.setProperty("userAlreadySigned",
					user != null && Utils.doesUserAlreadySignPetition(user, petition.getKey(), SEP));

			/*
			 * ArrayList<Map<String, String>> links = new ArrayList<>(); links.add(new
			 * HashMap<String, String>() { { put("rel", "self"); put("href", "/petition?id="
			 * + key); put("method", "GET"); } }); links.add(new HashMap<String, String>() {
			 * { put("rel", "sign"); put("href", "/petition?id=" + key); put("method",
			 * "PUT"); } }); petition.setProperty("links", links);
			 */

		} catch (EntityNotFoundException e) {
		}
		return petition;
	}

	@ApiMethod(name = "filterPetitions", httpMethod = HttpMethod.GET, path = "/petitions/filter")
	public CollectionResponse<Entity> getFilteredPetition(User user, @Nullable @Named("title") String title,
			@Nullable @Named("tag") List<String> tags, @Nullable @Named("next") String cursorString) {

		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		Filter filter = null;

		// Composite filter only allow multiple filters so simple filter should be
		// instantiated if there is only one tag in request
		if(tags != null && tags.size() == 1 && title == null) {
			filter = new FilterPredicate("tags", FilterOperator.EQUAL, tags.get(0));
		}else{
			Collection<Filter> filters = new ArrayList<>();
			if (tags != null) {
				for (String tag : tags) {
					filters.add(new FilterPredicate("tags", FilterOperator.EQUAL, tag));
				}
			}
			
			if (title != null) {
				filters.add(new FilterPredicate("name", FilterOperator.GREATER_THAN_OR_EQUAL, title));
				
				int charValue = title.charAt(title.length() - 1);
				String next = String.valueOf( (char) (charValue + 1));
				
				String notToOverLapName = title.substring(0, title.length() - 1) + next;
				filters.add(new FilterPredicate("name", FilterOperator.LESS_THAN, notToOverLapName));
			}

			filter = CompositeFilterOperator.and(filters);
		}

		Query q = new Query("Petition");
		if (filter != null) {
			q = q.setFilter(filter);
		}
		PreparedQuery pq = datastore.prepare(q);
		FetchOptions fetchOptions = FetchOptions.Builder.withLimit(10);

		if (cursorString != null) {
			fetchOptions.startCursor(Cursor.fromWebSafeString(cursorString));
		}

		QueryResultList<Entity> filteredPetitions = pq.asQueryResultList(fetchOptions);
		cursorString = filteredPetitions.getCursor().toWebSafeString();
		for (Entity p : filteredPetitions) {
			p.setProperty("userAlreadySigned",
					user != null && Utils.doesUserAlreadySignPetition(user, p.getKey(), SEP));
		}

		return CollectionResponse.<Entity>builder().setItems(filteredPetitions).setNextPageToken(cursorString).build();
	}

	// Connection required endpoints ↓
	@ApiMethod(name = "create", httpMethod = HttpMethod.POST, path = "/petitions")
	public Entity create(User user, Petition petition) throws UnauthorizedException {

		if (user == null) {
			throw new UnauthorizedException("Invalid credentials");
		}

		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		Entity createdPetition = Utils.createPetition(datastore, petition.name, petition.body, user.getEmail(),
				petition.tags, false, SEP);

		createdPetition.setProperty("userAlreadySigned", false);

		/*
		 * ArrayList<Map<String, String>> links = new ArrayList<>(); links.add(new
		 * HashMap<String, String>() { { put("rel", "self"); put("href", "/petition?id="
		 * + createdPetition.getKey().getName()); put("method", "GET"); } });
		 * createdPetition.setProperty("links", links);
		 */
		return createdPetition;
	}

	@ApiMethod(name = "getSignedPetition", httpMethod = HttpMethod.GET, path = "/me/signature")
	public CollectionResponse<Entity> getSignedPetition(User user, @Nullable @Named("next") String cursorString)
			throws UnauthorizedException, EntityNotFoundException {

		if (user == null) {
			throw new UnauthorizedException("Invalid credentials");
		}

		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		Query q = new Query("PetitionBucket")
				.setFilter(new FilterPredicate("voters", FilterOperator.EQUAL, user.getEmail()));

		PreparedQuery pq = datastore.prepare(q);

		FetchOptions fetchOptions = FetchOptions.Builder.withLimit(10);

		if (cursorString != null) {
			fetchOptions.startCursor(Cursor.fromWebSafeString(cursorString));
		}

		QueryResultList<Entity> signedPetitionBuckets = pq.asQueryResultList(fetchOptions);
		cursorString = signedPetitionBuckets.getCursor().toWebSafeString();

		List<Entity> signedPetitions = new ArrayList<>();
		for (Entity signedPetitionBucket : signedPetitionBuckets) {
			System.out.println(signedPetitionBucket);
			String petitionKey = Utils.extractKey(signedPetitionBucket);
			System.out.println(petitionKey);

			Entity petition = datastore.get(KeyFactory.createKey("Petition", petitionKey));
			petition.setProperty("userAlreadySigned", Utils.doesUserAlreadySignPetition(user, petition.getKey(), SEP));
			signedPetitions.add(petition);
		}
		return CollectionResponse.<Entity>builder().setItems(signedPetitions).setNextPageToken(cursorString).build();
	}

	@ApiMethod(name = "signPetition", httpMethod = HttpMethod.PUT, path = "/petition")
	public Entity signPetition(User user, @Named("id") String key)
			throws EntityNotFoundException, UnauthorizedException {

		if (user == null) {
			throw new UnauthorizedException("Invalid credentials");
		}

		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		Entity petition = datastore.get(KeyFactory.createKey("Petition", key));

		if (!Utils.doesUserAlreadySignPetition(user, petition.getKey(), SEP)) {
			int signCount = ((Long) petition.getProperty("signCount")).intValue();
			petition.setProperty("signCount", signCount + 1);
			datastore.put(petition);

			int randomInteger = Utils.getRandomValue(0, 9);
			Key petitionKey = KeyFactory.createKey("PetitionBucket", petition.getKey() + SEP + randomInteger);
			Entity petitionBucket = datastore.get(petitionKey);

			// TODO should ensure bucket voters < 40,000
			@SuppressWarnings("unchecked")
			ArrayList<String> voters = (ArrayList<String>) petitionBucket.getProperty("voters");
			if (voters == null) {
				voters = new ArrayList<>();
			}
			voters.add(user.getEmail());
			petitionBucket.setProperty("voters", voters);
			datastore.put(petitionBucket);
			System.out.println("vote");

		}
		petition.setProperty("userAlreadySigned", true);

		/*
		 * ArrayList<Map<String, String>> links = new ArrayList<>(); links.add(new
		 * HashMap<String, String>() { { put("rel", "self"); put("href", "/petition?id="
		 * + key); put("method", "GET"); } }); petition.setProperty("links", links);
		 */

		return petition;
	}

	@ApiMethod(name = "getCreatedPetition", httpMethod = HttpMethod.GET, path = "/me/petitions")
	public CollectionResponse<Entity> getCreatedPetition(User user, @Nullable @Named("next") String cursorString)
			throws UnauthorizedException {

		if (user == null) {
			throw new UnauthorizedException("Invalid credentials");
		}

		Query q = new Query("Petition").setFilter(new FilterPredicate("owner", FilterOperator.EQUAL, user.getEmail()));
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		PreparedQuery pq = datastore.prepare(q);

		FetchOptions fetchOptions = FetchOptions.Builder.withLimit(5);

		if (cursorString != null) {
			fetchOptions.startCursor(Cursor.fromWebSafeString(cursorString));
		}

		QueryResultList<Entity> createdPetitions = pq.asQueryResultList(fetchOptions);
		cursorString = createdPetitions.getCursor().toWebSafeString();

		for (Entity p : createdPetitions) {
			p.setProperty("userAlreadySigned", Utils.doesUserAlreadySignPetition(user, p.getKey(), SEP));
		}

		return CollectionResponse.<Entity>builder().setItems(createdPetitions).setNextPageToken(cursorString).build();
	}

	@ApiMethod(name = "endpoints", httpMethod = HttpMethod.GET, path = "/endpoints")
	public ArrayList<Map<String, String>> getendpoints() {

		ArrayList<Map<String, String>> endpoints = new ArrayList<>();
		endpoints.add(new HashMap<String, String>() {
			{
				put("name", "createFakePetitions");
				put("url", "/petitions/create/fake");
				put("method", "GET");
				put("authRequired", "false");
			}
		});
		endpoints.add(new HashMap<String, String>() {
			{
				put("name", "topPetitions");
				put("url", "/petitions/top100");
				put("method", "GET");
				put("authRequired", "false");
			}
		});
		endpoints.add(new HashMap<String, String>() {
			{
				put("name", "getFilteredPetition");
				put("url", "/petitions/filter");
				put("method", "GET");
				put("authRequired", "false");
			}
		});
		endpoints.add(new HashMap<String, String>() {
			{
				put("name", "getSinglePetition");
				put("url", "/petition");
				put("method", "GET");
				put("authRequired", "false");
			}
		});
		endpoints.add(new HashMap<String, String>() {
			{
				put("name", "create");
				put("url", "/petitions");
				put("method", "POST");
				put("authRequired", "true");
			}
		});
		endpoints.add(new HashMap<String, String>() {
			{
				put("name", "getSignedPetition");
				put("url", "/me/signature");
				put("method", "GET");
				put("authRequired", "true");
			}
		});
		endpoints.add(new HashMap<String, String>() {
			{
				put("name", "signPetition");
				put("url", "/petition");
				put("method", "PUT");
				put("authRequired", "true");
			}
		});
		endpoints.add(new HashMap<String, String>() {
			{
				put("name", "getCreatedPetition");
				put("url", "/me/petitions");
				put("method", "GET");
				put("authRequired", "true");
			}
		});
		endpoints.add(new HashMap<String, String>() {
			{
				put("name", "getAvailableTags");
				put("url", "/petitions/tags/list");
				put("method", "GET");
				put("authRequired", "false");
			}
		});

		return endpoints;
	}
}
