package com.chkpr.cyberthreat.collect;

import java.util.List;

/**
 * A source of items. Add a new source by implementing this interface and
 * annotating it with @Component - the scheduler discovers all of them.
 */
public interface Collector {

    /** Short, stable name shown as the source badge, e.g. "CISA KEV". */
    String name();

    /** Fetch the current batch of items from this source. */
    List<RawItem> collect();
}
