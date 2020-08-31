package com.jwdownload.upgrade;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
public enum STATUS_DOWNLOAD{

    DOWNLOADING(0,"Baixando...",10)
    ,PAUSED(1, "Pausado",10)
    ,COMPLETE(2, "Baixado",10)
    ,CANCELLED(3, "Cancelado",10)
    ,ERROR(4, "Erro!",10)
    ,UPDATED(5, "Atualizado",10)
    ,OUTDATED(6, "Desatualizado",20)
    ,NOT_DOWNLOADED(7, "Não Baixado",30)
    ,OUT_OF_SIZE(8, "Fora do tamanho",40);

    @Getter
    @Setter
    private int status;
    @Getter
    @Setter
    private String desc; 
    @Getter
    @Setter
    private int tableOrder;
    
    

};