package miage;

import java.io.IOException;
import java.util.Locale;

import org.junit.Test;

import com.github.javafaker.Faker;

public class HelloAppEngineTest {

	@Test
	public void test() throws IOException {
		Faker faker = new Faker(new Locale("fr"));
		System.out.println(faker.address().countryCode());
		System.out.println(faker.address().cityName());
	}
}
