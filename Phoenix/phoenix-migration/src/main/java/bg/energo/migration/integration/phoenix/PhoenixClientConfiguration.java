package bg.energo.migration.integration.phoenix;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class PhoenixClientConfiguration {
    //private final String token = "eyJraWQiOiJlcHJvLWRldi1rZXkiLCJ0eXAiOiJqd3QiLCJhbGciOiJSUzUxMiJ9.eyJpc3MiOiJFTkVSR08tUFJPIFBvcnRhbCIsInN1YiI6ImEzNjQ3OSIsImF1ZCI6WyJFTkVSR08tUFJPIEFQUFMiXSwiZXhwIjoxNzQ3Nzg5OTEwLCJuYmYiOjE3NDc3NTM5MTAsImlhdCI6MTc0Nzc1MzkxMCwianRpIjoiMTBiZWU4M2YtNDVhOS00ODljLTg5Y2EtYmQ5NzgxZTc4N2I5IiwidXNyIjpbXSwicG9ydGFsLWFkbWluIjpmYWxzZSwiYXBwTmFtZSI6IkVORVJHTy1QUk8gTG9naW4gUG9ydGFsIiwiYXBwSWQiOiJlNzc4ZjRjZS01MTBiLTQ1YWUtYjExMC0xM2IxMjA0MTdhYjIiLCJsYW5ndWFnZSI6ImVuX1VTIiwicHdyIjpbXSwiYWRtIjpbImU3NzhmNGNlLTUxMGItNDVhZS1iMTEwLTEzYjEyMDQxN2FiMiIsIjNkNzIxMDU5LTJmMDctNDVlNi04YTMyLWI2ZjkyNmY2NDQxNCJdLCJ0eXBlIjoiaWQifQ.ifDeJAm-x219rr3-vN6Khv_LoEOVZYPfy2ejYrgVrm7jUchV_srDUKcY4q4DUcpCuJC2JQl4JSsadIE67FpRYpK3R7F6fsKCXt2PPIEoCaaMWOgoiLLzz3wgs871g6iwjatdc4qtZkNjWICF6Es8r-u7ZcaVeqnHo6--8p7TVnPNH0lK0TzLYAD89ps5aB3xakRc0qCCyXEgorM442amg5d31RvKMphIkwA9xqyWPWcSvpNiRGn90o4ffPRlTC7CpOK1fLJjxbNFNhVFLK0n2FTerfRam-PHL20HSF5WGx2TOcG_8pw47Qlecck2hh7zb9zEZdMYbmml2L4cvlOGmECalxE8DaI61tAcWLmt_X8MjkjcyOSJVFv5UunLVHZDAb3d4o3DSN4Ez8JBraS745BotXwkJzxm_aZ9pQnZk4cogqxyQmqi5YClgL0tJ8nefQU95rGLM0rcdSPikRo8k9dzNH_6BWCcbTDVKGqIndfn4ijF-EqUhEBxhzL3hB8TdoZrP6LKzkc8s_Nl_3Dvi20MhcA_W5f6MruiS_qIR2d5041MeyuvOTvEkC4-a9vpczZqdwhXwtcRtqLwvjJZGi2LHohFp8XrkOGIFk-Fm__h1yplNKKWhnRhjRO23h8rG7-KfVHVMwcit6UTPOArN_ZPyA0XhMrxdk2TubxKtX8";
    private final String token = "eyJraWQiOiJlcHJvLWRldi1rZXkiLCJ0eXAiOiJqd3QiLCJhbGciOiJSUzUxMiJ9.eyJpc3MiOiJFTkVSR08tUFJPIFBvcnRhbCIsInN1YiI6InBob2VuaXgudGVzdGEiLCJhdWQiOlsiRU5FUkdPLVBSTyBBUFBTIiwiRVBSTyBQaG9lbml4Il0sImV4cCI6MTc2MzQ4NzcxMSwibmJmIjoxNzYzNDUxNzExLCJpYXQiOjE3NjM0NTE3MTEsImp0aSI6IjQyNzQ0Zjk0LWVmNjEtNGM0Yi1hM2Y4LWIzNDgyNGU1ZDI0YiIsImFwcE5hbWUiOiJFUFJPIFBob2VuaXgiLCJhcHBJZCI6IjNkNzIxMDU5LTJmMDctNDVlNi04YTMyLWI2ZjkyNmY2NDQxNCIsImxhbmd1YWdlIjoiYmdfQkciLCJ0eXBlIjoic3ZjIn0.eJLj5LicTpT9vUcZR8faOtvK3iib6H6Bc5DbWOCFFR1CoBbA957JTddMzwtoM-xyHOAT3HShS3Taf34wB2SGVCGemWWMQAiz42jleKYc1F4qHFrVDXEvzp6ZmqcrqgEHo4B-X7c_Qwx8uYap_XMNjSI550tM8p-LsgN9MUBnToyD7EMIfdCorKbGBxd-43y1a8ZX2FRYsUBEsVw22x-dez0XynomeOH8AO8vKdMle8TX611j6rJmEt1chN5RTKGlhBKjhY2pGq7NIRHizTIOs11Mh5avcTaMuaCZ27BU1s9R3ba5wMz-MNZREfljr64cJvbcNhJNcoZmAvfGr4RZBkakw1Bc6OHta6gf5Ww3Xg_h4a-_ZiDHwy8CFNiBtXL099J1ZwZ1V-ChkVSSyIezNgHPrt2Mf9Rbl4hnUBT28lFD-lq02f9aMXGFlMy91k80IMKdFUEPSGAnh3q1Qb-HjnTXNAXTYshEQrTZyJQylKGRaBMAwK4hPdkDuPBv86ETBqrFgW56lcomxbh_E9I1QLeAvbpdEXc6ooPMOt6Q6AwAUKWNKtlsN8dbL0awd4Kkc6nJYS9yrIcFmfCOSeCxU1u9Rj7omhjac6OIy6NLCGD2ff8hODiiVtrWlXVFlSLV_QsyyShGamcz6vzN5NXV3LPM6s7WvuiwxRG__oM7Zmo";

    @Bean("phoenixWebClient")
    public WebClient phoenixWebClient(WebClient.Builder builder) {
        return builder
                .baseUrl("http://10.236.20.31:7091/")
                .defaultHeader(
                        HttpHeaders.ACCEPT,
                        MediaType.APPLICATION_JSON_VALUE
                )
                .defaultHeader(
                        HttpHeaders.CONTENT_TYPE,
                        MediaType.APPLICATION_JSON_VALUE
                )
                .defaultHeader(
                        HttpHeaders.AUTHORIZATION,
                        "%s %s".formatted("Bearer ", token)
                )
                .build();
    }

}
