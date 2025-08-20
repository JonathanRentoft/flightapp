package dk.cphbusiness.flightdemo;

import com.fasterxml.jackson.databind.ObjectMapper;
import dk.cphbusiness.flightdemo.dtos.FlightDTO;
import dk.cphbusiness.flightdemo.dtos.FlightInfoDTO;
import dk.cphbusiness.utils.Utils;

import java.io.IOException;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Purpose: To read and process flight data from a JSON file.
 *
 * @author: Thomas Hartmann
 */
public class FlightReader {

    public static void main(String[] args) {
        try {
            List<FlightDTO> flightList = getFlightsFromFile("flights.json");

            // Get and print detailed flight info for each flight
            System.out.println("--- Detaljeret Flyinformation ---");
            List<FlightInfoDTO> flightInfoDTOList = getFlightInfoDetails(flightList);
            flightInfoDTOList.forEach(System.out::println);

            // Get and print the average flight time per airline
            System.out.println("\n--- Gennemsnitlig Flyvetid pr. Flyselskab ---");
            Map<String, Duration> averageFlightTimes = getAverageFlightTimeByAirline(flightList);
            averageFlightTimes.forEach((airline, avgDuration) -> {
                long hours = avgDuration.toHours();
                long minutes = avgDuration.toMinutesPart();
                System.out.printf("Flyselskab: %-25s | Gennemsnitlig varighed: %d timer, %d minutter%n", airline, hours, minutes);
            });

        } catch (IOException e) {
            System.err.println("Fejl under læsning eller behandling af flydatafilen: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Reads flight data from a JSON file and deserializes it into a list of FlightDTO objects.
     * @param filename The name of the JSON file.
     * @return A list of FlightDTO objects.
     * @throws IOException If there is an error reading the file.
     */
    public static List<FlightDTO> getFlightsFromFile(String filename) throws IOException {
        ObjectMapper objectMapper = Utils.getObjectMapper();
        // Assuming Utils.getObjectMapper() correctly configures the ObjectMapper for LocalDateTime
        // If not, you might need to add: objectMapper.registerModule(new JavaTimeModule());

        // Deserialize JSON from a file into FlightDTO[]
        FlightDTO[] flightsArray = objectMapper.readValue(Paths.get(filename).toFile(), FlightDTO[].class);

        // Convert array to a list
        return List.of(flightsArray);
    }

    /**
     * Converts a list of FlightDTOs to a list of FlightInfoDTOs with calculated durations.
     * @param flightList The list of flights to process.
     * @return A list of detailed flight information objects.
     */
    public static List<FlightInfoDTO> getFlightInfoDetails(List<FlightDTO> flightList) {
        return flightList.stream()
                .map(flight -> {
                    LocalDateTime departure = flight.getDeparture().getScheduled();
                    LocalDateTime arrival = flight.getArrival().getScheduled();
                    Duration duration = Duration.between(departure, arrival);
                    return FlightInfoDTO.builder()
                            .name(flight.getFlight().getNumber())
                            .iata(flight.getFlight().getIata())
                            .airline(flight.getAirline().getName())
                            .duration(duration)
                            .departure(departure)
                            .arrival(arrival)
                            .origin(flight.getDeparture().getAirport())
                            .destination(flight.getArrival().getAirport())
                            .build();
                })
                .toList();
    }

    /**
     * Calculates the average flight time for each airline in the provided list of flights.
     * @param flightList The list of flights to analyze.
     * @return A Map where the key is the airline name and the value is the average flight Duration.
     */
    public static Map<String, Duration> getAverageFlightTimeByAirline(List<FlightDTO> flightList) {
        return flightList.stream()
                // FIX: Add a filter to remove flights where the airline or its name is null
                .filter(flight -> flight.getAirline() != null && flight.getAirline().getName() != null)
                .collect(Collectors.groupingBy(
                        // Første argument: Hvad skal vi gruppere efter? Flyselskabets navn.
                        flight -> flight.getAirline().getName(),

                        // Andet argument: Hvad skal vi gøre med hver gruppe?
                        // Vi bruger collectingAndThen til at lave en efterbehandling.
                        Collectors.collectingAndThen(
                                // Først beregner vi gennemsnittet af varigheden i sekunder.
                                Collectors.averagingLong(flight ->
                                        Duration.between(
                                                flight.getDeparture().getScheduled(),
                                                flight.getArrival().getScheduled()
                                        ).toSeconds()),
                                // Bagefter (andThen) konverterer vi resultatet (Double) tilbage til en Duration.
                                averageSeconds -> Duration.ofSeconds(averageSeconds.longValue())
                        )
                ));
    }
}
