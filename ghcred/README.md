# GitHub Credential Helper

`startup.sh` creates a GitHub App JSON Web Token (JWT) and mints a short-lived installation access token. Both values are exported so subsequent commands can interact with the GitHub API.

## Usage

```bash
cd ghcred
./startup.sh
```

The script relies on the following environment variables (all have defaults):

- `APP_ID` – GitHub App ID (default `1786337`)
- `PEM_PATH` – path to the RSA private key (default `private-key.pem` in this directory)
- `INSTALLATION_ID_FILE` – cache for the installation ID (`installation_id.txt`)
- `DEBUG` – set to `1` for verbose output

On success it exports `JWT` and `TOKEN` for use in subsequent API calls.
