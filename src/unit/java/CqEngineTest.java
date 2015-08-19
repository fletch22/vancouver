import static com.googlecode.cqengine.query.QueryFactory.endsWith;
import static com.googlecode.cqengine.query.QueryFactory.equal;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fletch22.orb.search.Car;
import com.fletch22.orb.search.GeneratedClassFactory;
import com.googlecode.cqengine.ConcurrentIndexedCollection;
import com.googlecode.cqengine.IndexedCollection;
import com.googlecode.cqengine.attribute.SimpleNullableAttribute;
import com.googlecode.cqengine.index.navigable.NavigableIndex;
import com.googlecode.cqengine.index.standingquery.StandingQueryIndex;
import com.googlecode.cqengine.query.Query;
import com.googlecode.cqengine.resultset.ResultSet;

public class CqEngineTest {

	Logger logger = LoggerFactory.getLogger(CqEngineTest.class);
	
	Class<? extends SimpleNullableAttribute<Car, String>> clazz = null;

	@Test
	public void test() {
		// logger.info(AttributeSourceGenerator.generateAttributesForPastingIntoTargetClass(Car.class));
		IndexedCollection<Car> cars = new ConcurrentIndexedCollection<Car>();
		
		try {
			clazz = GeneratedClassFactory.getInstance();
		} catch (Exception e) {
			
			StackTraceElement[] trace = e.getStackTrace();
			for (StackTraceElement stackTraceElement: trace) {
				logger.info("CN: {}", stackTraceElement.getClassName());
			}
			throw new RuntimeException(e);
		}

		SimpleNullableAttribute<Car, String> thirdValue = null;
		try {
			thirdValue = (SimpleNullableAttribute<Car, String>) clazz.newInstance();
			logger.info("Type: {}", thirdValue.getClass().getName());
		} catch (Exception e) {
			e.printStackTrace();
		}

		cars.addIndex(NavigableIndex.onAttribute(thirdValue));
		// cars.addIndex(NavigableIndex.onAttribute(thirdValue));

		// 20-25% slower
		cars.addIndex(StandingQueryIndex.onQuery(equal(thirdValue, "Banana")));

		List<String> list = new ArrayList<String>();
		list.add("Pear");
		list.add("Apple");
		list.add("Banana");
		Car car1 = new Car(0, "red", list);
		cars.add(car1);

		int numberObjects = 1000000;
		for (int i = 1; i < numberObjects; i++) {
			cars.add(new Car(i, "red", list));
		}

		Query<Car> query1 = equal(thirdValue, "Banana");
		ResultSet<Car> resultSet = cars.retrieve(query1);
		logger.info("Found: {} cars", resultSet.size());
		for (Car car : resultSet) {
			 logger.info("Card ID found: {}", car.id);
		}

		// doQuery();

	}

	@SuppressWarnings("unused")
	private void doQuery() {
		// Arrange
		IndexedCollection<Car> cars = new ConcurrentIndexedCollection<Car>();
		cars.addIndex(NavigableIndex.onAttribute(Car.CAR_ID));

		List<String> list = new ArrayList<String>();
		list.add("Banana");

		int numberObjects = 1000000;
		for (int i = 0; i < numberObjects; i++) {
			Car car1 = new Car(i, "red", list);
			cars.add(car1);
		}

		// Act
		Query<Car> query1 = endsWith(Car.FIRST_CUSTOM_ATTRIBUTE, "Banana");
		ResultSet<Car> resultSet = cars.retrieve(query1);
		logger.info("Found: {} cars", resultSet.size());
		for (Car car : resultSet) {
			logger.info("Card ID found: {}", car.id);
		}
	}
	
}
