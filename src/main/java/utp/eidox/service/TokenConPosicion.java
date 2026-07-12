package utp.eidox.service;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class TokenConPosicion {
    private String tokenNormalizado;
    private int inicio;
    private int fin;
    private String tokenOriginal;
}