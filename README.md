# Atelier-PMD

This project integrates a version of [PMD](https://pmd.github.io/) designed to work with the Processing language into the [Atelier](https://github.com/creativeprogrammingatelier/atelier) system. The [PMD rules for Processing](https://github.com/ZITA4PDE/ProcessingPMD) used in this project are developed by Remco de Man and described in [this paper](https://doi.org/10.5220/0006701704200431). The conversion from Processing code to the Java code checked by PMD is written by Tim Blok for his [Zita](https://github.com/swordiemen/zita/) project, which is described in [this paper](http://purl.utwente.nl/essays/77948).

## Configuration

This project exposes a single API endpoint, namely a webhook to integrate with the Atelier system. To use the system, it needs to know about your instance of Atelier and this application has to be registered with Atelier as a plugin.

Configuration of Atelier-PMD is done using a JSON configuration file, an example of which is located in the `config/` folder. The location of the configuration file is set using the `ATELIER_PMD_CONFIG` environment variable. This configuration file contains all information the system needs to run as a plugin for Atelier:

- `atelierHost`: the url of your Atelier instance, without a trailing /
- `atelierPluginUserID`: the user ID you get in Atelier once you have registered the system as a plugin
- `webhookSecret`: a secret that Atelier will use to sign the webhook requests. Make sure that the value provided here is the same as the value configured in Atelier
- `publicKey`: the public key of this application, used for initial authentication
- `privateKey`: the private key corresponding with the public key

For each of these it is possible to get the values from an environment variable or a file on disk. To configure the host using an environment variable, use the following configuration:

```json
"atelierHost": "ENV::ATELIER_HOST"
```

This will use an environment variable called `ATELIER_HOST`. Similarly, you can use a file:

```json
"publicKey": "FILE::/run/secrets/keys/public.key"
```

If you specify a file path for the `publicKey` and `privateKey` variables and these files don't exist yet, Atelier-PMD will generate the keys and store them in the given locations.

## Running with Docker

You can build the Docker image for Atelier-PMD using the `docker build` command:

```sh
docker build . -t atelier-pmd
```

The image exposes one volume in which you have to provide your configuration: `/atelier-pmd/config`. By default the configuration inside the container is called `production.json`, but you can override it by setting the `ATELIER_PMD_CONFIG` environment variable. The Tomcat server is exposed on port 8080, and the Atelier-PMD application can be reached under the `/atelier-pmd` path.

## Debugging in IntelliJ

To debug Atelier-PMD in IntelliJ, you'll need to configure a Tomcat server in the *Run/Debug Configurations*. First create a new *Local Tomcat Server* configuration, and configure it to use your local Tomcat installation. Then you need to add the project to the server: go to the *Deployment* tab and add the `atelier-pmd:war exploded` artifact. Change the *Application context* to `/` to run the application at the root of the server. 

You also need to make sure you have a valid configuration for Atelier-PMD: copy the `config/example.json` to `config/development.json` and change it to reflect your local environment. Then in the *Startup/Connection* tab you need to set the `ATELIER_PMD_CONFIG` to the full path to this `development.json` file. Make sure to do this for both the *Run* and *Debug* configurations.

You can now start debugging the application by starting the Tomcat configuration.

