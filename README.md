# ContohPlugin REST API

This plugin provides a RESTful API to interact with your Nukkit server. You can get server information, see online players, and upload files.

## Installation

1.  Download the latest release from the [releases page](https://github.com/your-repo/ContohPlugin/releases).
2.  Place the downloaded `.jar` file into the `plugins` folder of your Nukkit server.
3.  Restart the server to load the plugin.

## API Endpoints

All endpoints are available under the base URL `http://<your-server-ip>:<port>/`. The default port is `8080`, but you can change this in the `config.yml` file.

*   **`GET /`**
    *   **Description:** A simple hello world endpoint.
    *   **Response:** `<h1>Hello World!</h1>`

*   **`GET /players`**
    *   **Description:** Get a list of online players.
    *   **Response:**
        ```json
        {
          "players": [
            {
              "uuid": 1234567890123456789,
              "name": "Player1"
            }
          ],
          "op": [
            {
              "uuid": 9876543210987654321,
              "name": "AdminPlayer"
            }
          ]
        }
        ```

*   **`GET /server`**
    *   **Description:** Get detailed information about the server, including RAM, CPU, storage, and online player count.
    *   **Response:**
        ```json
        {
          "ram": {
            "total": "512MB",
            "free": "256MB",
            "used": "256MB"
          },
          "cpu": {
            "processCpuLoad": 0.1,
            "systemCpuLoad": 0.5,
            "availableProcessors": 4
          },
          "storage": {
            "total": "100000MB",
            "free": "50000MB",
            "used": "50000MB"
          },
          "players": 2,
          "motd": "A Nukkit Server",
          "serverTime": "2023-10-27 10:00:00"
        }
        ```

*   **`POST /drop`**
    *   **Description:** Upload a file to the server. This is a `multipart/form-data` endpoint.
    *   **Form Data:**
        *   `file`: The file you want to upload.
    *   **Response:**
        ```json
        {
          "message": "File uploaded successfully to uploads/your-file.txt"
        }
        ```

## API Key System

You can protect your endpoints with an API key to prevent unauthorized access.

### Configuration

1.  Open the `config.yml` file located in the plugin's configuration folder.
2.  Find the `api-key` section:
    ```yaml
    api-key:
      enabled: false
      key: "your-secret-api-key"
      protected-endpoints:
        - "/players"
        - "/server"
        - "/drop"
    ```
3.  To enable the API key system, set `enabled` to `true`.
4.  Change the `key` to a strong, secret key of your choice.
5.  The `protected-endpoints` list contains all the endpoints that will require an API key. You can add or remove endpoints from this list.

### Usage

Once the API key system is enabled, you must include your API key in the `X-API-KEY` header for all requests to protected endpoints.

**Example using cURL:**

```bash
curl -X GET \
  http://<your-server-ip>:8080/players \
  -H 'X-API-KEY: your-secret-api-key'
```

If you do not provide a valid API key, you will receive a `401 Unauthorized` error.