
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.util.Optional;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import org.junit.jupiter.api.BeforeEach;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;



public class AppTest {

    @TempDir 
    Path tempDir;

    @BeforeEach
    void resetSharedState()
    {
        App.facts.clear();
    }

    @Test 
    void CatFactKeepsTheValuesItWasBuiltWith() 
    {
        App.CatFact fact = new App.CatFact(42, "Cats sleep a lot");

        assertEquals(42, fact.id());
        assertEquals("Cats sleep a lot", fact.fact());
    }

    @Test 
    void twoCatFactsWithSameValuesAreEqual()
    {
        App.CatFact a = new App.CatFact(1, "Cats purr");
        App.CatFact b = new App.CatFact(1, "Cats purr");

        assertEquals(a, b);
    }

    @Test 
    void twoCatFactsWithDifferentTextAreNotEqual()
    {
        App.CatFact a = new App.CatFact(1, "Cats purr");
        App.CatFact b = new App.CatFact(1, "Cats hiss");

        assertNotEquals(a, b);
    }

    @Test 
    void listFindsCatFactByValuesNotByObject()
    {
        List<App.CatFact> list = new ArrayList<>();
        list.add(new App.CatFact(1, "Cats purr"));
        
        assertTrue(list.contains(new App.CatFact(1, "Cats purr")));
    }

    @Test 
    void NewFactGivesBackItsText()
    {
        App.NewFact NewFact = new App.NewFact("Cats have whiskers");

        assertEquals("Cats have whiskers", NewFact.fact());
    }

    @Test 
    void ErrorBodyGivesBackItsMessage()
    {
        App.ErrorBody body = new App.ErrorBody("not found");

        assertEquals("not found", body.error());
    }

    @Test
    void CatFactCanHoldEmptyText()
    {
        App.CatFact fact = new App.CatFact(1, "");

        assertNotNull(fact);
        assertEquals("", fact.fact());
    }

    @Test 
    void serializedCatFactContainsBothFields()
    {
        App.CatFact fact = new App.CatFact(9, "Cats are curious");

        String json = App.gson.toJson(fact);

        assertTrue(json.contains("\"id\": 9"));
        assertTrue(json.contains("Cats are curious"));
    }

    @Test 
    void jsonBodyDeserializesIntoNewFact()
    {
        String json =  "{\"fact\": \"Cats have three eyelids\"}";

        App.NewFact NewFact = App.gson.fromJson(json, App.NewFact.class);

        assertEquals("Cats have three eyelids", NewFact.fact());
    }

    @Test 
    void apostropheIsNotEscapedByGson()
    {
        App.CatFact fact = new App.CatFact(1, "A cat's nose print is unique");

        String json = App.gson.toJson(fact);

        assertTrue(json.contains("cat's"));
        assertFalse(json.contains("\\u0027"));
    }

    @Test 
    void factWithCommasSurvivesRoundTrip()
    {
        App.CatFact original = new App.CatFact(9, "Cats, like people, tend to favor one paw over the other");

        String json = App.gson.toJson(original);
        App.CatFact restored = App.gson.fromJson(json, App.CatFact.class);

        assertEquals(original, restored);
    }

    @Test 
    void loadFromCsvReadsEveryRow() throws Exception
    {
        App.loadFromCsv();

        assertEquals(10, App.facts.size());
    }

    @Test 
    void factWithCommasIsStoredWhole() throws Exception
    {
        App.loadFromCsv();

        App.CatFact nine = null;
        for (App.CatFact f : App.facts)
        {
            if (f.id() == 9)
            {
                nine = f;
            }
        }

        assertNotNull(nine);
        assertEquals("Cats, like people, tend to favor one paw over the other", nine.fact());
    }

    @Test 
    void idsComeFromTheFile() throws Exception
    {
        App.loadFromCsv();

        assertEquals(1, App.facts.get(0).id());
    }

    @Test 
    void nextIdIsOnePastHighestIdInFile() throws Exception
    {
        App.loadFromCsv();

        assertEquals(11, App.nextId);
    }

    @Test 
    void loadingTwiceDoesNotDoubleTheFacts() throws Exception
    {
        App.loadFromCsv();
        int afterOneLoad = App.facts.size();

        App.loadFromCsv();

        assertEquals(afterOneLoad, App.facts.size());
    }

    /*
    Test 8 passes as CatFact record accepts empty text.
    Test 17 fails, it tired to add from loadFromCsv to App.facts without clearing first. */

    @Test 
    void getAllFactsReturnsEveryRowInIdOrder() throws Exception
    {
        Path db = tempDir.resolve("test.db");
        String url = "jdbc:sqlite:" + db;
        try (Connection c = DriverManager.getConnection(url);
            Statement s = c.createStatement())
        {
            s.executeUpdate("CREATE TABLE facts (id INTEGER PRIMARY KEY AUTOINCREMENT, fact TEXT NOT NULL)");
            s.executeUpdate("INSERT INTO facts (id, fact) VALUES (1, 'Cats purr')");
            s.executeUpdate("INSERT INTO facts (id, fact) VALUES (2, 'Cats, oddly, cannot taste sweetness')");
        }

        List<App.CatFact> facts = new App.FactsHandler().getAllFacts(url);

        assertEquals(2, facts.size());
        assertEquals(1, facts.get(0).id());
        assertEquals("Cats, oddly, cannot taste sweetness", facts.get(1).fact());
    }

    private String createTestDb() throws Exception
    {
        String url = "jdbc:sqlite:" + tempDir.resolve("test.db");
        try (Connection c = DriverManager.getConnection(url);
            Statement s = c.createStatement())
        {
            s.executeUpdate("CREATE TABLE facts (id INTEGER PRIMARY KEY AUTOINCREMENT, fact TEXT NOT NULL)");
            s.executeUpdate("INSERT INTO facts (id, fact) VALUES (1, 'Cats purr')");
            s.executeUpdate("INSERT INTO facts (id, fact) VALUES (2, 'Cats, oddly, cannot taste sweetness')");
        }
        
        return url;
    }

    @Test 
    void findFactByIdReturnsTheFactWhenTheIdExists() throws Exception
    {
        String url = createTestDb();

        Optional<App.CatFact> found = new App.FactsHandler().findFactById(url,2);

        assertTrue(found.isPresent());
        assertEquals(new App.CatFact(2, "Cats, oddly, cannot taste sweetness"), found.get());
    }

    @Test
    void findFactByIdReturnsEmptyWhenTheIdDoesNotExist() throws Exception
    {
        String url = createTestDb();

        Optional<App.CatFact> found = new App.FactsHandler().findFactById(url, 99);

        assertFalse(found.isPresent());
    }   
}
