package com.taobao.profile.test;

import com.taobao.profile.Manager;
import com.taobao.profile.Profiler;
import com.taobao.profile.dependence_query.SlowQueryData;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ProfilerTest {

    @Before
    public void setUp() {
        Manager.instance().setProfileFlag(true);
        Manager.setRecordTime(10);
        Manager.setNeedNanoTime(false);
        Profiler.clearData();
    }

    @Test
    public void testEnd4Mysql_shouldNotRecordWhenTimeIsBelowThreshold() {
        long threadId = Thread.currentThread().getId();
        Profiler.start4Mysql("host", 1234, "db", "sql");
        Profiler.end4Mysql();
        int threadIndex = (int) threadId;
        SlowQueryData thrData = Profiler.slowQueryProfile[threadIndex];
        assertEquals(0, thrData.profileData.size());
    }

    @Test
    public void testEnd4Mysql_shouldRecordWhenTimeIsAboveThreshold() throws InterruptedException {
        long threadId = Thread.currentThread().getId();
        Profiler.start4Mysql("host", 1234, "db", "sql");
        Thread.sleep(11);
        Profiler.end4Mysql();
        int threadIndex = (int) threadId;
        SlowQueryData thrData = Profiler.slowQueryProfile[threadIndex];
        assertEquals(1, thrData.profileData.size());
    }
}
