###Build docker image
```bash 
docker build -t app .
```

###Run docker image
```bash 
docker run -d -p 8081:8080 app
```