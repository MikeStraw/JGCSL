package org.gcsl.model;

import org.gcsl.sdif.SdifException;
import org.gcsl.sdif.SdifRec;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AthleteTest
{
    // The following for testing the hashcode and equals methods of Athlete
    @Test
    void testTwoIdenticalAthletesAreEqual()
    {
        Athlete a1 = new Athlete("John Smith", "M", "2000-01-01", 2);
        Athlete a2 = new Athlete("John Smith", "M", "2000-01-01", 2);
        assertEquals(a1, a2);

        Athlete a3 = new Athlete("John Smith", "M", "2000-01-01");
        a3.setTeamId(2);
        assertEquals(a1, a3);
    }

    @Test
    void testTwoDifferentAthletesAreNotEqual()
    {
        Athlete a1 = new Athlete("John Smith", "M", "2000-01-01", 2);
        Athlete a2 = new Athlete("John Smith", "M", "2000-01-01", 3);
        assertNotEquals(a1, a2);

        Athlete a3 = new Athlete("John Smith", "M", "2000-01-01", 2);
        Athlete a4 = new Athlete("John Smith", "M", "2001-01-01", 2);
        assertNotEquals(a3, a4);

        Athlete a5 = new Athlete("John Smith", "M", "2000-01-01", 2);
        Athlete a6 = new Athlete("John Smith", "F", "2000-01-01", 2);
        assertNotEquals(a5, a6);

        Athlete a7 = new Athlete("John Smith", "M", "2000-01-01", 2);
        Athlete a8 = new Athlete("John L. Smith", "M", "2000-01-01", 2);
        assertNotEquals(a7, a8);
    }


    @Test
    void testAddToSetDoesNotAllowMultiple()
    {
        Set<Athlete> athletes = new HashSet<>();
        Athlete a1 = new Athlete("John Smith", "M", "2000-01-01", 2);
        Athlete a2 = new Athlete("John Smith", "M", "2000-01-01", 2);
        athletes.add(a1);
        athletes.add(a2);
        assertEquals(1, athletes.size());
    }

    @Test
    void testRetrieveAthleteFromSet()
    {
        Set<Athlete> athletes = new HashSet<>();
        Athlete a1 = new Athlete("John Smith", "M", "2000-01-01", 2);
        Athlete a2 = new Athlete("Elmer Fudd", "M", "2010-01-01", 1);
        Athlete a3 = new Athlete("Buggs Bunny", "F", "1958-11-11", 4);
        athletes.add(a1);
        athletes.add(a2);
        athletes.add(a3);

        assertTrue(athletes.contains(a1));
        // a1 == a4
        Athlete a4 = new Athlete("John Smith", "M", "2000-01-01", 2);
        assertTrue(athletes.contains(a4));
    }

    // SDIF processing tests
    @Test
    void testMeetResultWithTimeNotNoShow()
    {
        String sdifData = "D08        Ertz, Lauren                            A   1120200115FF 1001 71 UN1807122017 1:00.24Y                   1:02.35Y     2 6     3       3  01      NN77\n";
        SdifRec sdifRec = new SdifRec(sdifData);

        try {
            Athlete noShowAthlete = Athlete.fromSdifMeetResult(sdifRec);
            assertFalse(noShowAthlete.isNoShow());

        } catch (SdifException e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    void testNoShowMeetResult()
    {
        String sdifData = "D08        Bucher, Josh                            A   0523200413MM   1H  8 UN1407122017 1:23.55Y                        NSY     1 4     0       0  06      NN37\n";
        SdifRec sdifRec = new SdifRec(sdifData);

        try {
            Athlete noShowAthlete = Athlete.fromSdifMeetResult(sdifRec);
            assertTrue(noShowAthlete.isNoShow());

        } catch (SdifException e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    void testScratchMeetResult()
    {
        String sdifData = "D08        Bucher, Josh                            A   0523200413MM   1H  8 UN1407122017 1:23.55Y                       SCRY     1 4     0       0  06      NN37\n";
        SdifRec sdifRec = new SdifRec(sdifData);

        try {
            Athlete scrAthlete = Athlete.fromSdifMeetResult(sdifRec);
            assertTrue(scrAthlete.isNoShow());

        } catch (SdifException e) {
            e.printStackTrace();
            fail();
        }
    }
}