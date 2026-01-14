package galois.udf;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import speedy.model.algebra.udf.IUserDefinedFunction;
import speedy.model.database.Tuple;

@RequiredArgsConstructor
@Slf4j
public class GaloisUDMap implements IUserDefinedFunction {
    @Override
    public Object execute(Tuple tuple) {
        // TODO: implementare funzione con più parametri opzionali
        // udmap("Sentimento dal testo {1}", r.text)
        // udmap("'Vero' se il volo {1} con destinazioni {2} atterra in Germania", r.company_name, r.destinations)
        // udmap("Uno score da 1 a 5")
        // udmap("Dal procedimento {1} estra la lista degli ingredienti come array JSON") -> "[\"acqua\", \"farina\", \"uova\"]"
        return "TODO: implement";
    }
}
