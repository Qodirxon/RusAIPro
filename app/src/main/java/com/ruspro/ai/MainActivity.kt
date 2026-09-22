
package com.ruspro.ai

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import org.json.JSONArray
import java.util.Locale

data class Mission(
    val id:String,val domain:String,val emoji:String,val level:String,
    val topic:String,val prompt:String,val goal:String
)

class Prefs(context:Context){
    private val p=context.getSharedPreferences("ruspro_final",Context.MODE_PRIVATE)
    var xp:Int get()=p.getInt("xp",0) set(v){p.edit().putInt("xp",v).apply()}
    var streak:Int get()=p.getInt("streak",1) set(v){p.edit().putInt("streak",v).apply()}
    var done:Int get()=p.getInt("done",0) set(v){p.edit().putInt("done",v).apply()}
    var level:String get()=p.getString("level","A2")?: "A2" set(v){p.edit().putString("level",v).apply()}
    var grammar:Int get()=p.getInt("grammar",0) set(v){p.edit().putInt("grammar",v).apply()}
    var vocab:Int get()=p.getInt("vocab",0) set(v){p.edit().putInt("vocab",v).apply()}
    var relevance:Int get()=p.getInt("relevance",0) set(v){p.edit().putInt("relevance",v).apply()}
    var fluency:Int get()=p.getInt("fluency",0) set(v){p.edit().putInt("fluency",v).apply()}
    var theme:Int get()=p.getInt("theme",0) set(v){p.edit().putInt("theme",v).apply()}
}

fun loadMissions(ctx:Context):List<Mission>{
    val txt=ctx.assets.open("missions.json").bufferedReader().use{it.readText()}
    val a=JSONArray(txt)
    return List(a.length()){i->
        val o=a.getJSONObject(i)
        Mission(o.getString("id"),o.getString("domain"),o.getString("emoji"),
            o.getString("level"),o.getString("topic"),o.getString("prompt"),o.getString("goal"))
    }
}

class Voice(private val ctx:Context, private val onText:(String)->Unit){
    private var sr:SpeechRecognizer?=null
    private var tts:TextToSpeech?=null
    init{ tts=TextToSpeech(ctx){tts?.language=Locale("ru","RU")}}
    fun speak(s:String){tts?.speak(s,TextToSpeech.QUEUE_FLUSH,null,"ruspro")}
    fun listen(){
        if(!SpeechRecognizer.isRecognitionAvailable(ctx))return
        sr?.destroy()
        sr=SpeechRecognizer.createSpeechRecognizer(ctx)
        sr?.setRecognitionListener(object:RecognitionListener{
            override fun onResults(b:Bundle?){b?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let(onText)}
            override fun onError(e:Int){}
            override fun onReadyForSpeech(b:Bundle?){}
            override fun onBeginningOfSpeech(){}
            override fun onRmsChanged(v:Float){}
            override fun onBufferReceived(b:ByteArray?){}
            override fun onEndOfSpeech(){}
            override fun onPartialResults(b:Bundle?){}
            override fun onEvent(t:Int,b:Bundle?){}
        })
        sr?.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply{
            putExtra(RecognizerIntent.EXTRA_LANGUAGE,"ru-RU")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        })
    }
    fun close(){sr?.destroy();tts?.shutdown()}
}

@Composable
fun FinalApp(){
    val ctx=LocalContext.current
    val prefs=remember{Prefs(ctx)}
    val missions=remember{loadMissions(ctx)}
    var tab by remember{mutableStateOf(0)}
    var selected by rememberSaveable{mutableStateOf<String?>(null)}
    var xp by remember{mutableStateOf(prefs.xp)}
    var voiceText by remember{mutableStateOf("")}
    var feedback by remember{mutableStateOf("")}
    val voice=remember{Voice(ctx){voiceText=it}}
    val mic=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){}

    DisposableEffect(Unit){onDispose{voice.close()}}

    MaterialTheme(colorScheme=lightColorScheme(primary=Color(0xFF4F46E5),secondary=Color(0xFF0EA5E9))){
        Scaffold(bottomBar={
            NavigationBar{
                val labels=listOf("Главная","Миссии","Review","Прогресс")
                val icons=listOf(Icons.Default.Home,Icons.Default.Explore,Icons.Default.Style,Icons.Default.Insights)
                labels.forEachIndexed{i,s->NavigationBarItem(selected=tab==i,onClick={tab=i;selected=null},icon={Icon(icons[i],null)},label={Text(s)})}
            }
        }){pad->
            Box(Modifier.padding(pad).fillMaxSize()){
                when{
                    selected!=null->MissionScreen(missions.firstOrNull{it.id==selected}!!,voiceText,feedback,
                        onSpeak={voice.speak(it)},onVoice={
                            if(ctx.checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED)voice.listen()
                            else mic.launch(Manifest.permission.RECORD_AUDIO)
                        },onCheck={
                            val score=scoreAnswer(voiceText)
                            prefs.xp+=score; prefs.done++; xp=prefs.xp
                            if(score<20)prefs.grammar++
                            feedback=if(score>=35)"🔥 Zo‘r! Juda tabiiy va mazmunli javob." else "🧠 ${coach(voiceText)}"
                        },onBack={selected=null; voiceText=""; feedback=""})
                    tab==0->Home(prefs,xp,onStart={tab=1; selected=null})
                    tab==1->Missions(missions,prefs,onOpen={selected=it.id})
                    tab==2->Review()
                    else->Progress(prefs,xp)
                }
            }
        }
    }
}

fun scoreAnswer(t:String):Int{
    if(t.isBlank())return 0
    val n=t.trim().split(Regex("\\s+")).size
    val polite=listOf("пожалуйста","понимаю","предлагаю","согласен","уточнить","думаю","потому").count{t.lowercase().contains(it)}
    return (10+n*2+polite*4).coerceAtMost(50)
}
fun coach(t:String):String{
    if(t.isBlank())return "Javobni ovoz bilan aytib ko‘r."
    if(t.split(Regex("\\s+")).size<8)return "Javobni kamida 1–2 to‘liq gapga kengaytir."
    if(!t.contains("потому")&&!t.contains("поэтому")&&!t.contains("предлагаю"))
        return "Bog‘lovchi yoki taklif qo‘sh: «потому что», «поэтому», «предлагаю»."
    return "Yaxshi. Keyingi safar aniqroq misol va sabab qo‘sh."
}

@Composable
fun Home(p:Prefs,xp:Int,onStart:()->Unit){
    LazyColumn(Modifier.fillMaxSize().padding(18.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
        item{Text("RusPro AI",fontSize=32.sp,fontWeight=FontWeight.Black);Text("v2.0 FINAL • Русский для реальной жизни",color=Color.Gray)}
        item{Card(shape=RoundedCornerShape(26.dp),colors=CardDefaults.cardColors(containerColor=Color(0xFFEEF0FF))){
            Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
                Text("Текущий уровень",color=Color.Gray);Text(p.level,fontSize=42.sp,fontWeight=FontWeight.Black)
                Text("$xp XP   •   🔥 ${p.streak} дней   •   ${p.done} миссий")
                Button(onClick=onStart,Modifier.fillMaxWidth()){Text("Продолжить →")}
            }
        }}
        item{Card{Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
            Text("🎯 Сегодня",fontSize=20.sp,fontWeight=FontWeight.Bold)
            Text("12 минут: 1 миссия + 5 слов + 1 голосовой ответ + Boss.")
            Text("Adaptive Coach: grammar ${p.grammar} • vocab ${p.vocab} • relevance ${p.relevance}")
        }}}
        item{Text("🏆 Достижения",fontSize=20.sp,fontWeight=FontWeight.Bold)}
        item{Text("🔓 Первые 10 XP   •   ${if(p.done>=10)"🔓 10 миссий" else "🔒 10 миссий"}   •   ${if(p.streak>=7)"🔓 7-day streak" else "🔒 7-day streak"}")}
    }
}


@Composable
fun Missions(all:List<Mission>,p:Prefs,onOpen:(Mission)->Unit){
    var filter by rememberSaveable{mutableStateOf("Все")}
    val visible=remember(filter,all){
        all.asSequence()
            .filter{filter=="Все" || it.domain==filter}
            .take(100)
            .toList()
    }
    val filters=listOf("Все","Work","Travel","Interview","Negotiation","Customer Service")
    Column(Modifier.fillMaxSize().padding(horizontal=16.dp)){
        Column(Modifier.padding(top=16.dp,bottom=8.dp)){
            Text("520 миссий",fontSize=28.sp,fontWeight=FontWeight.Bold)
            Text("Tanla → gapir → xatoni tuzat → XP ol.",color=Color.Gray)
        }
        androidx.compose.foundation.lazy.LazyRow(
            horizontalArrangement=Arrangement.spacedBy(6.dp),
            contentPadding=PaddingValues(bottom=10.dp)
        ){
            items(filters){f->
                FilterChip(selected=filter==f,onClick={filter=f},label={Text(f)})
            }
        }
        LazyColumn(
            verticalArrangement=Arrangement.spacedBy(10.dp),
            contentPadding=PaddingValues(bottom=18.dp)
        ){
            items(visible,key={it.id}){m->
                Card(
                    Modifier.fillMaxWidth().clickable{onOpen(m)},
                    shape=RoundedCornerShape(18.dp)
                ){
                    Row(Modifier.padding(15.dp),verticalAlignment=Alignment.CenterVertically){
                        Text(m.emoji,fontSize=28.sp)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)){
                            Text("${m.level} • ${m.domain}",fontWeight=FontWeight.Bold)
                            Text(m.topic,maxLines=1)
                            Text(m.goal,color=Color.Gray,fontSize=12.sp,maxLines=2)
                        }
                        Icon(Icons.Default.ChevronRight,null)
                    }
                }
            }
        }
    }
}


@Composable
fun MissionScreen(
    m:Mission,voiceText:String,feedback:String,
    onSpeak:(String)->Unit,onVoice:()->Unit,onCheck:()->Unit,onBack:()->Unit
){
    Column(Modifier.fillMaxSize()){
        Row(
            Modifier.fillMaxWidth().padding(horizontal=8.dp,vertical=6.dp),
            verticalAlignment=Alignment.CenterVertically
        ){
            IconButton(onClick=onBack){Icon(Icons.Default.ArrowBack,null)}
            Text("${m.emoji} ${m.domain} • ${m.level}",fontWeight=FontWeight.Bold,maxLines=1)
        }
        LazyColumn(
            Modifier.fillMaxSize().padding(horizontal=16.dp),
            verticalArrangement=Arrangement.spacedBy(14.dp),
            contentPadding=PaddingValues(bottom=24.dp)
        ){
            item{
                Card(shape=RoundedCornerShape(24.dp)){
                    Column(Modifier.padding(22.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
                        Text(m.topic,fontSize=28.sp,fontWeight=FontWeight.Black)
                        Text(m.prompt,fontSize=19.sp)
                        Text("Цель: ${m.goal}",color=Color.Gray)
                        OutlinedButton(onClick={onSpeak(m.prompt)}) {
                            Icon(Icons.Default.VolumeUp,null)
                            Spacer(Modifier.width(6.dp))
                            Text("Слушать")
                        }
                    }
                }
            }
            item{
                OutlinedButton(
                    onClick=onVoice,
                    Modifier.fillMaxWidth()
                ){
                    Icon(Icons.Default.Mic,null)
                    Spacer(Modifier.width(6.dp))
                    Text("🎙️ Ответить по-русски")
                }
            }
            if(voiceText.isNotBlank()){
                item{
                    Card{
                        Text("Распознано:\n$voiceText",Modifier.padding(16.dp))
                    }
                }
            }
            item{
                Button(
                    onClick=onCheck,
                    enabled=voiceText.isNotBlank(),
                    Modifier.fillMaxWidth()
                ){Text("Проверить + XP")}
            }
            if(feedback.isNotBlank()){
                item{
                    Card(colors=CardDefaults.cardColors(containerColor=Color(0xFFEFF6FF))){
                        Text(feedback,Modifier.padding(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun Review(){
    val words=listOf("уточнить — aniqlashtirmoq","срок — muddat","договорённость — kelishuv","обеспокоенность — tashvish","предложить — taklif qilmoq","промежуточный — oraliq")
    LazyColumn(Modifier.fillMaxSize().padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
        item{Text("Spaced Review",fontSize=28.sp,fontWeight=FontWeight.Bold);Text("Har kuni 5–10 ta foydali so‘zni real gapda ishlat.",color=Color.Gray)}
        items(words){w->Card(Modifier.fillMaxWidth()){Text(w,Modifier.padding(18.dp),fontSize=20.sp,fontWeight=FontWeight.SemiBold)}}
    }
}

@Composable
fun Progress(p:Prefs,xp:Int){
    LazyColumn(Modifier.fillMaxSize().padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
        item{Text("Progress & Coach",fontSize=28.sp,fontWeight=FontWeight.Bold)}
        item{Card{Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
            Text("CEFR ${p.level}",fontSize=30.sp,fontWeight=FontWeight.Black)
            Text("$xp XP • ${p.done} missions • 🔥 ${p.streak} days")
            LinearProgressIndicator(progress={((xp%100)/100f)},Modifier.fillMaxWidth())
        }}}
        item{Text("🧬 Error DNA",fontSize=21.sp,fontWeight=FontWeight.Bold)}
        item{Text("Grammar: ${p.grammar}\nVocabulary: ${p.vocab}\nRelevance: ${p.relevance}\nFluency: ${p.fluency}",fontSize=17.sp)}
        item{Text("🏆 Final roadmap",fontWeight=FontWeight.Bold,fontSize=20.sp)}
        item{Text("A1 → A2 → B1 → B2 → C1 → Boss Mastery\nMaqsad: real hayotdagi ruscha suhbatni avtomatlashtirish.")}
    }
}

class MainActivity:ComponentActivity(){
    override fun onCreate(b:Bundle?){super.onCreate(b);setContent{FinalApp()}}
}
