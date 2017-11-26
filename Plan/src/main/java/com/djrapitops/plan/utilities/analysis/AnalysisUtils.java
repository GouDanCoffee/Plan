package main.java.com.djrapitops.plan.utilities.analysis;

import com.djrapitops.plugin.api.TimeAmount;
import com.djrapitops.plugin.api.utility.log.Log;
import main.java.com.djrapitops.plan.api.IPlan;
import main.java.com.djrapitops.plan.data.Session;
import main.java.com.djrapitops.plan.data.time.GMTimes;
import main.java.com.djrapitops.plan.data.time.WorldTimes;
import main.java.com.djrapitops.plan.utilities.MiscUtils;
import main.java.com.djrapitops.plan.utilities.comparators.SessionLengthComparator;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Rsl1122
 */
public class AnalysisUtils {

    /**
     * Constructor used to hide the public constructor
     */
    private AnalysisUtils() {
        throw new IllegalStateException("Utility class");
    }

    @Deprecated
    public static boolean isActive(long now, long lastPlayed, long playTime, long loginTimes) {
        int timeToActive = 10;
        long twoWeeks = 1209600000;
        return now - lastPlayed < twoWeeks
                && loginTimes > 3
                && playTime > 60 * timeToActive;
    }

    public static long getNewPlayers(List<Long> registered, long scale, long now) {
        long newPlayers = 0;
        if (!registered.isEmpty()) {
            newPlayers = registered.stream()
                    .filter(Objects::nonNull)
                    .filter(reg -> reg > now - scale)
                    .count();
        }
        // Filters out register dates before scale
        return newPlayers;
    }

    @Deprecated
    public static List<Long> transformSessionDataToLengths(Collection<Session> data) {
        return data.stream()
                .filter(Objects::nonNull)
                .filter(session -> session.getLength() > 0)
                .map(Session::getLength)
                .collect(Collectors.toList());
    }

    /**
     * Used to calculate unique players that have played within the time frame determined by scale.
     *
     * @param sessions All sessions sorted in a map by User's UUID
     * @param scale    Scale (milliseconds), time before (Current epoch - scale) will be ignored.
     * @return Amount of Unique joins within the time span.
     */
    @Deprecated
    public static int getUniqueJoins(Map<UUID, List<Session>> sessions, long scale) {
        long now = MiscUtils.getTime();
        long nowMinusScale = now - scale;

        Set<UUID> uniqueJoins = new HashSet<>();
        sessions.forEach((uuid, s) ->
                s.stream()
                        .filter(session -> session.getSessionStart() >= nowMinusScale)
                        .map(session -> uuid)
                        .forEach(uniqueJoins::add)
        );

        return uniqueJoins.size();
    }

    public static int getUniqueJoinsPerDay(Map<UUID, List<Session>> sessions, long after) {
        Map<Integer, Set<UUID>> uniqueJoins = new HashMap<>();

        sessions.forEach((uuid, s) -> {
            for (Session session : s) {
                if (session.getSessionStart() < after) {
                    continue;
                }

                int day = getDayOfYear(session);

                uniqueJoins.computeIfAbsent(day, computedDay -> new HashSet<>());
                uniqueJoins.get(day).add(uuid);
            }
        });

        int total = MathUtils.sumInt(uniqueJoins.values().stream().map(Set::size));
        int numberOfDays = uniqueJoins.size();

        if (numberOfDays == 0) {
            return 0;
        }

        return total / numberOfDays;
    }

    public static long getNewUsersPerDay(List<Long> registers, long after, long total) {
        Set<Integer> days = new HashSet<>();
        for (Long date : registers) {
            if (date < after) {
                continue;
            }
            int day = getDayOfYear(date);
            days.add(day);
        }
        int numberOfDays = days.size();

        if (numberOfDays == 0) {
            return 0;
        }
        return total / numberOfDays;
    }

    /**
     * Transforms the session start list into a list of int arrays.
     * <p>
     * First number signifies the Day of Week. (0 = Monday, 6 = Sunday)
     * Second number signifies the Hour of Day. (0 = 0 AM, 23 = 11 PM)
     *
     * @param sessionStarts List of Session start Epoch ms.
     * @return list of int arrays.
     */
    public static List<int[]> getDaysAndHours(List<Long> sessionStarts) {
        return sessionStarts.stream().map((Long start) -> {
            Calendar day = Calendar.getInstance();
            day.setTimeInMillis(start);
            int hourOfDay = day.get(Calendar.HOUR_OF_DAY); // 0 AM is 0
            int dayOfWeek = day.get(Calendar.DAY_OF_WEEK) - 2; // Monday is 0, Sunday is -1
            if (hourOfDay == 24) { // Condition if hour is 24 (Should be impossible but.)
                hourOfDay = 0;
                dayOfWeek += 1;
            }
            if (dayOfWeek > 6) { // If Hour added a day on Sunday, move to Monday
                dayOfWeek = 0;
            }
            if (dayOfWeek < 0) { // Move Sunday to 6
                dayOfWeek = 6;
            }
            return new int[]{dayOfWeek, hourOfDay};
        }).collect(Collectors.toList());
    }

    public static int getDayOfYear(Session session) {
        return getDayOfYear(session.getSessionStart());

    }

    public static int getDayOfYear(long date) {
        Calendar day = Calendar.getInstance();
        day.setTimeInMillis(date);
        return day.get(Calendar.DAY_OF_YEAR);
    }

    public static double getAveragePerDay(long after, long before, long total) {
        return total / getNumberOfDaysBetween(after, before);
    }

    public static long getNumberOfDaysBetween(long start, long end) {
        long value = 0;
        long test = start;
        long day = TimeAmount.DAY.ms();
        while (test < end) {
            test += day;
            value++;
        }
        return value;
    }

    @Deprecated
    public static long getTotalPlaytime(List<Session> sessions) {
        return sessions.stream().mapToLong(Session::getLength).sum();
    }

    @Deprecated
    public static long getLongestSessionLength(List<Session> sessions) {
        Optional<Session> longest = sessions.stream().sorted(new SessionLengthComparator()).findFirst();
        return longest.map(Session::getLength).orElse(0L);
    }

    @Deprecated
    public static long getLastSeen(List<Session> userSessions) {
        OptionalLong max = userSessions.stream().mapToLong(Session::getSessionEnd).max();
        if (max.isPresent()) {
            return max.getAsLong();
        }
        return 0;
    }

    public static void addMissingWorlds(WorldTimes worldTimes) {
        try {
            // Add 0 time for worlds not present.
            Set<String> nonZeroWorlds = worldTimes.getWorldTimes().keySet();
            IPlan plugin = MiscUtils.getIPlan();
            for (String world : plugin.getDB().getWorldTable().getWorldNames(plugin.getServerUuid())) {
                if (nonZeroWorlds.contains(world)) {
                    continue;
                }
                worldTimes.setGMTimesForWorld(world, new GMTimes());
            }
        } catch (SQLException e) {
            Log.toLog("AnalysisUtils.addMissingWorlds", e);
        }
    }
}
