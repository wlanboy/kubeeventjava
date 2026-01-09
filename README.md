POD=$(kubectl get pod -n kubeevent -l app=kubeevent -o jsonpath='{.items[0].metadata.name}')

mirrord exec -t pod/$POD -n kubeevent  -- mvn spring-boot:run