package fr.adrien.aeroclock;

/** Maps Open-Meteo WMO weather codes to a scene and a French label. */
final class WeatherCodes {

    private WeatherCodes() {}

    /** Scene names match the wx_&lt;scene&gt;_d / wx_&lt;scene&gt;_n drawables. */
    static String scene(int code) {
        switch (code) {
            case 0: case 1:
                return "clear";
            case 2:
                return "partly";
            case 3:
                return "cloudy";
            case 45: case 48:
                return "fog";
            case 51: case 53: case 55: case 56: case 57:
                return "drizzle";
            case 61: case 63: case 65: case 66: case 67:
            case 80: case 81: case 82:
                return "rain";
            case 71: case 73: case 75: case 77: case 85: case 86:
                return "snow";
            case 95: case 96: case 99:
                return "storm";
            default:
                return "cloudy";
        }
    }

    static String label(int code, boolean day) {
        switch (code) {
            case 0: return day ? "Ensoleillé" : "Nuit claire";
            case 1: return day ? "Plutôt ensoleillé" : "Plutôt dégagé";
            case 2: return "Partiellement nuageux";
            case 3: return "Couvert";
            case 45: return "Brouillard";
            case 48: return "Brouillard givrant";
            case 51: return "Bruine légère";
            case 53: return "Bruine";
            case 55: return "Bruine forte";
            case 56: case 57: return "Bruine verglaçante";
            case 61: return "Pluie faible";
            case 63: return "Pluie";
            case 65: return "Forte pluie";
            case 66: case 67: return "Pluie verglaçante";
            case 71: return "Neige faible";
            case 73: return "Neige";
            case 75: return "Forte neige";
            case 77: return "Grains de neige";
            case 80: return "Averses";
            case 81: return "Averses";
            case 82: return "Fortes averses";
            case 85: case 86: return "Averses de neige";
            case 95: return "Orage";
            case 96: case 99: return "Orage et grêle";
            default: return "Nuageux";
        }
    }
}
