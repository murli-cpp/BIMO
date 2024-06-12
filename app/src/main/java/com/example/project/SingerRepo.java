package com.example.project;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by paetztm on 2/6/2017.
 */

public class SingerRepo {
    //solo
    public List<Singer> getSingerList() {
        List<Singer> singerList = new ArrayList<>();
        singerList.add(new Singer("Solo", "1995.04", "Lim ChangJung"));

        singerList.add(new Singer("FIN.K.L", "1998.05", "Lee Jin"));
        singerList.add(new Singer("FIN.K.L", "1998.05", "Sung YuRi"));
        singerList.add(new Singer("FIN.K.L", "1998.05", "Oak JooHyun"));
        singerList.add(new Singer("FIN.K.L", "1998.05", "Lee HyoRi"));

        singerList.add(new Singer("Solo", "1999.04", "Kim BumSoo"));

        singerList.add(new Singer("Solo", "1999.11", "Park HyoShin"));
        singerList.add(new Singer("Solo", "1999.11", "Lee SooYoung"));
        singerList.add(new Singer("Solo", "2000.11", "Sung SiKyung"));

        singerList.add(new Singer("Buzz", "2003.10", "Kim Yeah"));
        singerList.add(new Singer("Buzz", "2003.10", "Yun WooHyun"));
        singerList.add(new Singer("Buzz", "2003.10", "Sin JunKi"));
        singerList.add(new Singer("Buzz", "2003.10", "Min KyungHoon"));

        singerList.add(new Singer("Solo", "2006.06", "Yunha"));

        singerList.add(new Singer("Girls' Generation", "2007.08", "TaeYeon"));
        singerList.add(new Singer("Girls' Generation", "2007.08", "Sunny"));
        singerList.add(new Singer("Girls' Generation", "2007.08", "Tiffany"));
        singerList.add(new Singer("Girls' Generation", "2007.08", "HyoYeon"));
        singerList.add(new Singer("Girls' Generation", "2007.08", "YuRi"));
        singerList.add(new Singer("Girls' Generation", "2007.08", "SooYoung"));
        singerList.add(new Singer("Girls' Generation", "2007.08", "YoonA"));
        singerList.add(new Singer("Girls' Generation", "2007.08", "SeoHyun"));

        singerList.add(new Singer("Wanna One", "2017.08", "Kang Daniel"));
        singerList.add(new Singer("Wanna One", "2017.08", "Lai Kuan Lin"));
        singerList.add(new Singer("Wanna One", "2017.08", "Ong SeongWu"));
        singerList.add(new Singer("Wanna One", "2017.08", "Ha SungWoon"));
        singerList.add(new Singer("Wanna One", "2017.08", "Yoon JiSung"));
        singerList.add(new Singer("Wanna One", "2017.08", "Park WooJin"));
        singerList.add(new Singer("Wanna One", "2017.08", "Lee DaeHwi"));
        singerList.add(new Singer("Wanna One", "2017.08", "Kim JaeHwan"));
        singerList.add(new Singer("Wanna One", "2017.08", "Bae JinYoung"));
        singerList.add(new Singer("Wanna One", "2017.08", "Hwang MinHyun"));
        singerList.add(new Singer("Wanna One", "2017.08", "Park JiHoon"));

        singerList.add(new Singer("Solo", "2017.11", "Woo WonJae"));

        return singerList;
    }
}

