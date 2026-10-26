
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;



public class AppTest {

    @Test 
    void catFactKeepsTheValuesItWasBuiltWith() 
    {
        App.catFact fact = new App.catFact(42, "Cats sleep a lot");

        assertEquals(42, fact.id());
        assertEquals("Cats sleep a lot", fact.fact());
    }

    @Test 
    void twoCatFactsWithSameValuesAreEqual()
    {
        App.catFact a = new App.catFact(1, "Cats purr");
        App.catFact b = new App.catFact(1, "Cats purr");

        assertEquals(a, b);
    }

    @Test 
    void twoCatFactsWithDifferentTextAreNotEqual()
    {
        App.catFact a = new App.catFact(1, "Cats purr");
        App.catFact b = new App.catFact(1, "Cats hiss");

        assertNotEquals(a, b);
    }

    @Test 
    void listFindsCatFactByValuesNotByObject()
    {
        List<App.catFact> list = new ArrayList<>();
        list.add(new App.catFact(1, "Cats purr"));
        
        assertTrue(list.contains(new App.catFact(1, "Cats purr")));
    }

    @Test 
    void newFactGivesBackItsText()
    {
        App.newFact newFact = new App.newFact("Cats have whiskers");

        assertEquals("Cats have whiskers", newFact.fact());
    }

    @Test 
    void errorBodyGivesBackItsMessage()
    {
        App.errorBody body = new App.errorBody("not found");

        assertEquals("not found", body.error());
    }

    @Test
    void catFactCanHoldEmptyText()
    {
        App.catFact fact = new App.catFact(1, "");

        assertNotNull(fact);
        assertEquals("", fact.fact());
    }

    @Test 
    void serializedCatFactContainsBothFields()
    {
        App.catFact fact = new App.catFact(9, "Cats are curious");

        String json = App.gson.toJson(fact);

        assertTrue(json.contains("\"id\": 9"));
        assertTrue(json.contains("Cats are curious"));
    }

    @Test 
    void jsonBodyDeserializesIntoNewFact()
    {
        String json =  "{\"fact\": \"Cats have three eyelids\"}";

        App.newFact newFact = App.gson.fromJson(json, App.newFact.class);

        assertEquals("Cats have three eyelids", newFact.fact());
    }

    @Test 
    void apostropheIsNotEscapedByGson()
    {
        App.catFact fact = new App.catFact(1, "A cat's nose print is unique");

        String json = App.gson.toJson(fact);

        assertTrue(json.contains("cat's"));
        assertFalse(json.contains("\\u0027"));
    }

    @Test 
    void factWithCommasSurvivesRoundTrip()
    {
        App.catFact original = new App.catFact(9, "Cats, like people, tend to favor one paw over the other");

        String json = App.gson.toJson(original);
        App.catFact restored = App.gson.fromJson(json, App.catFact.class);

        assertEquals(original, restored);
    }
}
