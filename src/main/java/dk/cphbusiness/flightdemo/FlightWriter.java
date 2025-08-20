package dk.cphbusiness.flightdemo;

import dk.cphbusiness.flightdemo.dtos.FlightCollectionDTO;
import dk.cphbusiness.flightdemo.dtos.FlightDTO;
import dk.cphbusiness.utils.Utils;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;


/**
 * Purpose:
 *
 * @author: Thomas Hartmann
 */
public class FlightWriter {
    private final String FLIGHT_URL = "https://api.aviationstack.com/v1/flights?access_key=%s&limit=%s&offset=%s";
    private Utils utils = new Utils();

    /**
     * Method to write flights from the aviation stack API to a file
     * @throws IOException
     */
    public List<FlightDTO> writeFlightsToFile(int numberOfRequests, int limit) throws IOException {
        List<FlightDTO> result = urlReader(FLIGHT_URL, numberOfRequests, limit);
        jsonToFile(result, "flightsfile.json");
        return result;
    }
    private List<FlightDTO> urlReader(String urlString, int numberOfRequests, int limit) throws IOException {
        List<FlightDTO> flightDTOList = new ArrayList();
        List<FlightCollectionDTO> flightCollectionDTOList = new ArrayList<>();

        int offset = 0;
        FlightCollectionDTO flights = null;
        
        for (int i = 0; i < numberOfRequests; i++) {
            urlString = String.format(urlString, new Utils().getPropertyValue("aviation.key"), limit, offset);
            URL url = new URL(urlString);
            flights = utils.getObjectMapper()
                    .readValue(url, FlightCollectionDTO.class);
            flightCollectionDTOList.add(flights);
            offset += limit;
        }

        flightCollectionDTOList.forEach(flightCollectionDTO -> {
            for (FlightDTO flightDTO : flightCollectionDTO.getData()) {
                flightDTOList.add(flightDTO);
            }
        });
        return flightDTOList;
    }

    private void jsonToFile(List<FlightDTO> flightCollection, String fileName) throws IOException {
        utils.getObjectMapper().writeValue(Paths.get(fileName).toFile(), flightCollection);
    }

}
