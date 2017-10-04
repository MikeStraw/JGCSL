package org.gcsl.model;

import org.gcsl.util.Utils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TeamTest
{
    @Test
    void testAddAthlete()
    {
        Athlete a1 = new Athlete("Buggs Bunny", "M", "1958-11-11");
        Athlete a2 = new Athlete("Elmer Fudd", "M", "157-01-01");
        Team team = new Team(1, "short", "long name", "right-now");
        team.addAthlete(a1);
        team.addAthlete(a2);
        assertEquals(2, team.getAthletes().size());
    }

    @Test
    void testRemoveAthlete()
    {
        Athlete a1 = new Athlete("Buggs Bunny", "M", "1958-11-11");
        Athlete a2 = new Athlete("Elmer Fudd", "M", "157-01-01");
        Team team = new Team(1, "short", "long name", "right-now");
        team.addAthlete(a1);
        team.addAthlete(a2);
        team.removeAthlete(a1);
        assertEquals(1, team.getAthletes().size());
    }

    @Test
    void testSetIdAddsIdToAthletes()
    {
        Athlete a1 = new Athlete("Buggs Bunny", "M", "1958-11-11");
        Athlete a2 = new Athlete("Elmer Fudd", "M", "157-01-01");
        Team team = new Team(Utils.INVALID_ID, "short", "long name", "right-now");

        team.addAthlete(a1);
        team.addAthlete(a2);

        int teamId = 3;
        team.setId(teamId);

        for (Athlete a : team.getAthletes()) {
            assertEquals(teamId, a.getTeamId());
        }
    }
}