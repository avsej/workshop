/**
 * Copyright (c) 2015 Couchbase, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALING
 * IN THE SOFTWARE.
 */
package com.couchbase.workshop;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.query.Query;
import com.couchbase.client.java.query.QueryResult;
import com.couchbase.client.java.query.QueryRow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.ArrayList;
import java.util.List;

import static com.couchbase.client.java.query.Select.select;
import static com.couchbase.client.java.query.dsl.Expression.i;
import static com.couchbase.client.java.query.dsl.Expression.s;
import static com.couchbase.client.java.query.dsl.Expression.x;
import static com.couchbase.client.java.query.dsl.Sort.asc;

@RestController
@RequestMapping("/flightpath")
public class FlightpathController {

    private final Bucket bucket;

    @Autowired
    public FlightpathController(Bucket bucket) {
        this.bucket = bucket;
    }

    /**
     * Finds all the flight paths between two airports (FAA codes)
     *
     * If you only have a primary index, this query will take 30 seconds or so! Speed it up with
     * these two indexes:
     *
     *  - create index idx_dest on `travel-sample` (destinationairport);
     *  - create index idx_source on `travel-sample` (sourceairport);
     *
     * They will bring it down to a few hundred ms...
     *
     * @param from faa code from source
     * @param to faa code from destination
     * @return the list of flight paths
     */
    @RequestMapping("/all")
    public List<FlightPath> findAll(@RequestParam String from, @RequestParam String to) {
       QueryResult result = bucket.query(Query.simple(
           select("a.name", "s.flight", "s.utc", "r.equipment")
               .from(i(bucket.name()).as("r"))
               .unnest("r.schedule s")
               .join(i(bucket.name()).as("a").toString()).onKeys("r.airlineid")
               .where(x("r.sourceairport").eq(s(from)).and(x("r.destinationairport").eq(s(to))))
               .orderBy(asc("s.utc"))
       ));

        List<FlightPath> flightPaths = new ArrayList<>();
        for (QueryRow rows : result) {
            flightPaths.add(FlightPath.create(rows.value(), from, to));
        }
        return flightPaths;
    }

}
