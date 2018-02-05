package com.aamalik.gtfs.neo4j;

import com.aamalik.gtfs.neo4j.dto.TripPlan;
import com.aamalik.gtfs.neo4j.entity.*;
import com.aamalik.gtfs.neo4j.entity.projection.stoptime.TripPlanResultProjection;
import com.aamalik.gtfs.neo4j.repository.*;
import org.neo4j.graphdb.Path;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Controller
@RequestMapping("/customrest")
public class Neo4jWebServiceController {

    @Autowired
    AgencyRepository agencyRepository;

    @Autowired
    RouteRepository routeRepository;

    @Autowired
    StopRepository stopRepository;

    @Autowired
    StoptimeRepository stoptimeRepository;

    @Autowired
    CalendarDateRepository calendarDateRepository;

    @Autowired
    TripRepository tripRepository;

    @Autowired
    ProjectionFactory projectionFactory;


    @GetMapping(path = "/agency/{agencyId}", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    //Example id: HVV
    public Agency getAgency(@PathVariable String agencyId, Model model) {
        return agencyRepository.findByAgencyId(agencyId,1);
    }
;
    @GetMapping(path = "/agency/{agencyId}/routes", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    //Example id: HVV
    public Set<Route> getAgencyRoutes(@PathVariable String agencyId, Model model) {
        Agency agency = agencyRepository.findByAgencyId(agencyId,1);
        return agency.routes;
    }

    @GetMapping(path = "/route/{routeId}", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    //Example id: 13
    public Route getRoute(@PathVariable String routeId, Model model) {
        return routeRepository.findByRouteId(routeId,1);
    }

    @GetMapping(path = "/getStuffDone/{routeId}", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public Path getStuffDone(@PathVariable String routeId, Model model) {
        Route route = routeRepository.findByRouteId(routeId,1);

        Stop s1 = stopRepository.findByName("U Schlump",2);
        Stop s2 = stopRepository.findByName("Sartoriusstraße",2);

        return null;
    }


    @GetMapping(path = "/stop/{stopName}", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    //Example name: U Schlump
    public Stop getStop(@PathVariable String stopName, Model model) {
        return stopRepository.findByName(stopName,1);
    }

    @GetMapping(path = "/stoptime/{id}", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    //Example id: 1270015
    public Stoptime getStopTime(@PathVariable Long id, Model model) {
        stoptimeRepository.findOne(id,1);
        return null;
    }

    @GetMapping(path = "/trip/{tripId}", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    //Example id: 22
    public Trip getTrip(@PathVariable String tripId, Model model) {
        return tripRepository.findByTripId(tripId, 1);
    }

    @RequestMapping(value = "/planTripNoTransfer", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ArrayList <ArrayList <ArrayList<Stoptime>>> planTripNoTransfer(@RequestBody TripPlan plan){

        ArrayList <ArrayList <ArrayList<Stoptime>>> allPlansWithLegs = new ArrayList<>();

        Sort sort = new Sort(Sort.Direction.ASC, "tripId").
                and( new Sort(Sort.Direction.ASC, "departureTimeInt"));
        Pageable pageable = new PageRequest(0, 1000000, sort);

        Page<Stoptime> imResult = stoptimeRepository.getMyTrips(
                plan.getTravelDate(),
                plan.getOrigStation(),
                plan.getOrigArrivalTimeLow(),
                plan.getOrigArrivalTimeHigh(),
                plan.getDestStation(),
                plan.getDestArrivalTimeLow(),
                plan.getDestArrivalTimeHigh(),
                pageable);

        ArrayList <ArrayList<Stoptime>> finalResult = breakupTrips( imResult);

        //Single leg trip
        for (ArrayList<Stoptime> leg : finalResult) {
            ArrayList <ArrayList<Stoptime>> planWithLegs = new ArrayList<>();
            planWithLegs.add(leg);
            allPlansWithLegs.add((planWithLegs));
        }
        return allPlansWithLegs;
    }

    @RequestMapping(value = "/allshortestpaths", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ArrayList <ArrayList <ArrayList<Stoptime>>>  allshortestpaths( @RequestBody TripPlan plan){

        ArrayList<ArrayList<ArrayList<Stoptime>>> allPlansWithLegs = new ArrayList<>();

        Pageable pageable = new PageRequest(0, 1000, null);

        Page<Stoptime> imResult = stoptimeRepository.allshortestpaths(
                plan.getOrigStation(),
                plan.getDestStation(),
                pageable);

        System.out.println(imResult);
        ArrayList <ArrayList<Stoptime>> finalResult = breakupTrips( imResult);

        System.out.println(finalResult.size());

        for (ArrayList<Stoptime> leg : finalResult) {
            ArrayList <ArrayList<Stoptime>> planWithLegs = new ArrayList<>();
            planWithLegs.add(leg);
            allPlansWithLegs.add((planWithLegs));
        }

        return allPlansWithLegs;
    }

    @RequestMapping(value = "/shortestPath", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ArrayList <ArrayList <ArrayList<Stoptime>>> basicRoutePost(@RequestBody TripPlan plan){

        ArrayList <ArrayList <ArrayList<Stoptime>>> allPlansWithLegs = new ArrayList<>();

        Sort sort = new Sort(Sort.Direction.ASC, "stopid");
        Pageable pageable = new PageRequest(0, 1000, sort);

        Page<Stoptime> resultlist = stoptimeRepository.shortestPath(
                        plan.getOrigStation(),
                        plan.getDestStation(),
                        pageable);

        ArrayList <ArrayList<Stoptime>> finalResult = breakupTrips( resultlist);

        for (ArrayList<Stoptime> leg : finalResult) {
            ArrayList <ArrayList<Stoptime>> planWithLegs = new ArrayList<>();
            planWithLegs.add(leg);
            allPlansWithLegs.add((planWithLegs));
        }

        return allPlansWithLegs;
    }

    @RequestMapping(value = "/specificTrip", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ArrayList <ArrayList <ArrayList<Stoptime>>> specificRoute(@RequestBody TripPlan plan){

        ArrayList <ArrayList <ArrayList<Stoptime>>> allPlansWithLegs = new ArrayList<>();

        Sort sort = new Sort(Sort.Direction.ASC, "stopid");
        Pageable pageable = new PageRequest(0, 100000, sort);

        Page<Stoptime> imResult = stoptimeRepository.specificTrip(
                plan.getOrigStation(),
                plan.getDestStation(),
                pageable);

        ArrayList <ArrayList<Stoptime>> finalResult = breakupTrips( imResult);

        for (ArrayList<Stoptime> leg : finalResult) {
            ArrayList <ArrayList<Stoptime>> planWithLegs = new ArrayList<>();
            planWithLegs.add(leg);
            allPlansWithLegs.add((planWithLegs));
        }

        return allPlansWithLegs;
    }

    private  ArrayList <ArrayList<Stoptime>> breakupDistance(Page<Stoptime> test) {

        ArrayList <ArrayList<Stoptime>> result = new ArrayList<>();

        String lastTripId = null;
        ArrayList currentSet = null;

        System.out.println("Content: " + test.getContent());

        for (Stoptime stoptime: test.getContent()) {

//            System.out.println("stoptime: " + stoptime);
            System.out.println("stoptime: " + stoptime.getTrips());

            if(stoptime.getTrips() != null) {
                Trip currentTrip = stoptime.getTrips().iterator().next();

                if (lastTripId == null || !lastTripId.equals(currentTrip.getTripId())) {
                    currentSet = new ArrayList<Stoptime>();
                    result.add(currentSet);
                    lastTripId = currentTrip.getTripId();
                }

                System.out.println("currentSet: " + currentSet);
                TripPlanResultProjection tripPlan = projectionFactory.createProjection(TripPlanResultProjection.class, stoptime);

                currentSet.add(tripPlan);
            }
        }
        return result;

    }



    private  ArrayList <ArrayList<Stoptime>> breakupTrips(Page<Stoptime> test) {
        return breakupTrips(test.getContent());
    }

    private  ArrayList <ArrayList<Stoptime>> breakupTrips(List<Stoptime> test) {
        ArrayList <ArrayList<Stoptime>> result = new ArrayList<>();

        String lastTripId = null;
        ArrayList currentSet = null;

        for (Stoptime stoptime: test) {

            if(stoptime.getTrips() != null) {
                Trip currentTrip = stoptime.getTrips().iterator().next();

                if (lastTripId == null || !lastTripId.equals(currentTrip.getTripId())) {
                    currentSet = new ArrayList<Stoptime>();
                    result.add(currentSet);
                    lastTripId = currentTrip.getTripId();
                }

                TripPlanResultProjection tripPlan = projectionFactory.createProjection(TripPlanResultProjection.class, stoptime);

                currentSet.add(tripPlan);
            }
        }
        return result;
    }

//    private Path testGraphAlgoFactory(Stop s1, Stop s2){
//
//        PathExpander expander = PathExpanders.allTypesAndDirections();
//        PathFinder<Path> pathFinder =
//                GraphAlgoFactory.shortestPath(expander , 6);
//
//        Path path = pathFinder.findSinglePath(s1, s2.);
//        System.out.println("path: " + path);
//
//        return path;
//    }
}
